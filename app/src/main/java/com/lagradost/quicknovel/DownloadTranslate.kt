package com.lagradost.quicknovel

import com.lagradost.quicknovel.util.translation.TranslationManager
import com.lagradost.quicknovel.util.translation.TranslationsUtils
import com.lagradost.quicknovel.util.translation.models.TranslatorAgents
import org.jsoup.Jsoup
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeVisitor

/** Engine used when translating chapters before EPUB/PDF export. */
enum class DownloadTranslateEngine {
    Gemini,
    GoogleOnline,
}

/**
 * Checks that [translated] is not just the original text (engines sometimes "succeed"
 * by echoing the source). Used so downloads never ship the original language by mistake.
 */
object DownloadTranslationGuard {
    fun plainText(htmlOrText: String): String =
        Jsoup.parseBodyFragment(htmlOrText).text().replace(Regex("\\s+"), " ").trim()

    fun looksTranslated(source: LoadedChapter, translated: LoadedChapter): Boolean {
        val src = plainText(source.html).ifBlank { source.title.trim() }
        val dst = plainText(translated.html).ifBlank { translated.title.trim() }
        if (src.isBlank() || dst.isBlank()) return false
        if (src.equals(dst, ignoreCase = true)) return false
        // Very short chapters: any change is enough.
        if (src.length < 40) return !src.equals(dst, ignoreCase = true)
        val srcWords = src.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
        val dstWords = dst.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
        if (srcWords.isEmpty() || dstWords.isEmpty()) return false
        val shared = srcWords.intersect(dstWords).size
        val union = srcWords.union(dstWords).size
        val jaccard = shared.toDouble() / maxOf(union, 1)
        // Real translations diverge a lot; cosmetic edits keep most of the same words.
        return jaccard < 0.78
    }

    /**
     * True when the chapter already reads as [targetCode] (e.g. Spanish → "es").
     * Skip the translator in that case — Google/Gemini often echo the source and look like a failure.
     */
    fun alreadyInLanguage(chapter: LoadedChapter, targetCode: String): Boolean {
        val wanted = targetCode.trim().lowercase().take(2)
        if (wanted.isBlank()) return false
        val sample = buildString {
            append(chapter.title)
            append(' ')
            append(plainText(chapter.html))
        }
        val detected = com.lagradost.quicknovel.util.TtsVoicePicker.detectLanguage(sample)
        return detected != null && detected.equals(wanted, ignoreCase = true)
    }
}

/**
 * One language the user can pick in the download dialog.
 * [code] is ISO-639-1 for Google Translate; [englishName] is used in the Gemini prompt.
 */
data class DownloadTranslateLang(
    val code: String,
    val englishName: String,
    val displayName: String,
)

/** Languages offered for download translation (Spanish stays first after “none”). */
object DownloadTranslateLangs {
    val ALL: List<DownloadTranslateLang> = listOf(
        DownloadTranslateLang("es", "Spanish", "Español"),
        DownloadTranslateLang("en", "English", "English"),
        DownloadTranslateLang("pt", "Portuguese", "Português"),
        DownloadTranslateLang("fr", "French", "Français"),
        DownloadTranslateLang("de", "German", "Deutsch"),
        DownloadTranslateLang("it", "Italian", "Italiano"),
        DownloadTranslateLang("ru", "Russian", "Русский"),
        DownloadTranslateLang("zh", "Chinese", "中文"),
        DownloadTranslateLang("ja", "Japanese", "日本語"),
        DownloadTranslateLang("ko", "Korean", "한국어"),
        DownloadTranslateLang("ar", "Arabic", "العربية"),
        DownloadTranslateLang("hi", "Hindi", "हिन्दी"),
        DownloadTranslateLang("id", "Indonesian", "Bahasa Indonesia"),
        DownloadTranslateLang("tr", "Turkish", "Türkçe"),
        DownloadTranslateLang("vi", "Vietnamese", "Tiếng Việt"),
        DownloadTranslateLang("pl", "Polish", "Polski"),
        DownloadTranslateLang("uk", "Ukrainian", "Українська"),
        DownloadTranslateLang("th", "Thai", "ไทย"),
    )

    fun byCode(code: String): DownloadTranslateLang? =
        ALL.firstOrNull { it.code.equals(code, ignoreCase = true) }
}

/** What the download dialog asked for: no translation, or a language + engine. */
data class DownloadTranslateRequest(
    val lang: DownloadTranslateLang,
    val engine: DownloadTranslateEngine,
) {
    val cacheSuffix: String
        get() = when (engine) {
            DownloadTranslateEngine.Gemini -> "${lang.code}.gemini"
            DownloadTranslateEngine.GoogleOnline -> "${lang.code}.gtx"
        }
}

/**
 * Translates a chapter HTML with the existing Google Translate online path
 * (same stack as the in-reader online translator).
 */
object OnlineHtmlChapterTranslator {
    suspend fun translateChapter(chapter: LoadedChapter, toCode: String): LoadedChapter {
        if (DownloadTranslationGuard.alreadyInLanguage(chapter, toCode)) {
            return chapter
        }
        val doc = Jsoup.parseBodyFragment(chapter.html)
        doc.outputSettings().prettyPrint(false)
        val nodes = ArrayList<TextNode>()
        doc.body().traverse(object : NodeVisitor {
            override fun head(node: Node, depth: Int) {
                if (node is TextNode && TranslationsUtils.isTranslatable(node.wholeText, false)) {
                    nodes.add(node)
                }
            }

            override fun tail(node: Node, depth: Int) = Unit
        })

        val inputs = ArrayList<String>(nodes.size + 1)
        inputs.add(chapter.title)
        nodes.forEach { inputs.add(it.wholeText) }

        val manager = TranslationManager()
        val out = try {
            manager.translate(
                textList = inputs,
                from = "auto",
                to = toCode,
                agent = TranslatorAgents.ONLINE,
            )
        } catch (e: Exception) {
            throw TranslationException(e.message ?: e.javaClass.simpleName)
        } finally {
            manager.release()
        }

        val title = out.firstOrNull()?.takeIf { it.isNotBlank() } ?: chapter.title
        nodes.forEachIndexed { index, node ->
            val piece = out.getOrNull(index + 1) ?: return@forEachIndexed
            // Keep original node if the online engine echoed the source for this fragment.
            if (piece.isNotBlank()) node.text(piece)
        }
        val result = LoadedChapter(title.trim(), sanitizeChapterHtmlForEpub(doc.body().html()))
        if (!DownloadTranslationGuard.looksTranslated(chapter, result)) {
            throw TranslationException(
                "Google Translate returned the original text for ${toCode.uppercase()}"
            )
        }
        return result
    }
}
