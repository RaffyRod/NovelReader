package com.lagradost.quicknovel.providers

import com.lagradost.quicknovel.ChapterData
import com.lagradost.quicknovel.ErrorLoadingException
import com.lagradost.quicknovel.LoadResponse
import com.lagradost.quicknovel.MainAPI
import com.lagradost.quicknovel.SearchResponse
import com.lagradost.quicknovel.newSearchResponse
import com.lagradost.quicknovel.newStreamResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URLEncoder

/**
 * A provider described only by CSS selectors, so a site can be added without writing code.
 * Selectors that are not required are left empty.
 */
data class CustomProviderConfig(
    val name: String,
    val baseUrl: String,
    val lang: String = "en",
    /** Search page, `{query}` is replaced by the search text. May be relative to [baseUrl]. */
    val searchUrl: String,
    val resultItem: String,
    val resultLink: String = "a",
    val resultImage: String = "",
    val novelTitle: String,
    val novelAuthor: String = "",
    val novelCover: String = "",
    val novelSynopsis: String = "",
    val chapterLink: String,
    val chaptersNewestFirst: Boolean = false,
    /** Madara sites list the chapters with a POST to `<novel>/ajax/chapters/`. */
    val chaptersViaAjax: Boolean = false,
    val content: String,
) {
    companion object {
        /** Selectors of the WordPress Madara theme, used by a large number of novel sites. */
        fun madaraTemplate(name: String = "", baseUrl: String = "") = CustomProviderConfig(
            name = name,
            baseUrl = baseUrl,
            searchUrl = "/?s={query}&post_type=wp-manga",
            resultItem = "div.c-tabs-item__content",
            resultLink = "div.post-title a",
            resultImage = "div.tab-thumb img",
            novelTitle = "div.post-title h1",
            novelAuthor = ".author-content a",
            novelCover = "div.summary_image img",
            novelSynopsis = "div.summary__content",
            chapterLink = ".wp-manga-chapter > a[href]",
            chaptersNewestFirst = true,
            chaptersViaAjax = true,
            content = "div.reading-content",
        )
    }
}

enum class ConfigProblem {
    EmptyName,
    InvalidUrl,
    SearchUrlWithoutQuery,
    MissingSelector,
    InvalidSelector,
}

data class ConfigCheck(val problem: ConfigProblem, val detail: String = "")

data class ParsedResult(val name: String, val url: String, val poster: String?)
data class ParsedChapter(val name: String, val url: String)
data class ParsedNovel(
    val name: String,
    val author: String?,
    val poster: String?,
    val synopsis: String?,
)

/** Turns pages into data using a [CustomProviderConfig], with no network access. */
object CustomProviderParser {
    private val imageAttributes = listOf("data-src", "data-lazy-src", "data-original", "src")
    private const val DEFAULT_LINK = "a"

    /** Returns the first problem of the config, or null when it can be used. */
    fun validate(config: CustomProviderConfig, takenNames: Set<String> = emptySet()): ConfigCheck? {
        if (config.name.isBlank() || config.name in takenNames) {
            return ConfigCheck(ConfigProblem.EmptyName, config.name)
        }
        val uri = runCatching { URI(config.baseUrl.trim()) }.getOrNull()
        if (uri == null || uri.host.isNullOrBlank() || uri.scheme !in listOf("http", "https")) {
            return ConfigCheck(ConfigProblem.InvalidUrl, config.baseUrl)
        }
        if (!config.searchUrl.contains("{query}")) {
            return ConfigCheck(ConfigProblem.SearchUrlWithoutQuery)
        }
        val required = mapOf(
            "result" to config.resultItem,
            "title" to config.novelTitle,
            "chapters" to config.chapterLink,
            "content" to config.content,
        )
        required.forEach { (label, selector) ->
            if (selector.isBlank()) return ConfigCheck(ConfigProblem.MissingSelector, label)
        }
        val selectors = listOf(
            config.resultItem, config.resultLink, config.resultImage, config.novelTitle,
            config.novelAuthor, config.novelCover, config.novelSynopsis, config.chapterLink,
            config.content
        ).filter { it.isNotBlank() }
        selectors.forEach { selector ->
            val valid = runCatching { Jsoup.parse("<p></p>").select(selector) }.isSuccess
            if (!valid) return ConfigCheck(ConfigProblem.InvalidSelector, selector)
        }
        return null
    }

    fun searchUrl(config: CustomProviderConfig, query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return absolute(config.baseUrl, config.searchUrl.replace("{query}", encoded))
    }

    /** Resolves [url] against the site root, accepting absolute, protocol relative and root relative urls. */
    fun absolute(baseUrl: String, url: String): String {
        val base = baseUrl.trim().trimEnd('/')
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> base + url
            else -> "$base/$url"
        }
    }

    fun parseSearch(doc: Document, config: CustomProviderConfig): List<ParsedResult> {
        val linkSelector = config.resultLink.ifBlank { DEFAULT_LINK }
        return doc.select(config.resultItem).mapNotNull { item ->
            val link = (if (item.`is`(linkSelector)) item else item.selectFirst(linkSelector))
                ?: return@mapNotNull null
            val url = link.absUrl("href").ifBlank { return@mapNotNull null }
            val name = link.attr("title").ifBlank { link.text() }.trim()
            if (name.isBlank()) return@mapNotNull null
            val poster = config.resultImage.takeIf { it.isNotBlank() }
                ?.let { item.selectFirst(it) }
                ?.let { imageUrl(it) }
            ParsedResult(name, url, poster)
        }.distinctBy { it.url }
    }

    fun parseNovel(doc: Document, config: CustomProviderConfig): ParsedNovel? {
        val name = doc.selectFirst(config.novelTitle)?.text()?.trim()?.takeIf { it.isNotBlank() }
            ?: return null
        fun text(selector: String) = selector.takeIf { it.isNotBlank() }
            ?.let { doc.selectFirst(it) }?.text()?.trim()?.takeIf { it.isNotBlank() }
        return ParsedNovel(
            name = name,
            author = text(config.novelAuthor),
            poster = config.novelCover.takeIf { it.isNotBlank() }
                ?.let { doc.selectFirst(it) }
                ?.let { imageUrl(it) },
            synopsis = text(config.novelSynopsis),
        )
    }

    /** Chapters in reading order, whatever order the site lists them in. */
    fun parseChapters(doc: Document, config: CustomProviderConfig): List<ParsedChapter> {
        val chapters = doc.select(config.chapterLink).mapNotNull { link ->
            val url = link.absUrl("href").ifBlank { return@mapNotNull null }
            val name = link.text().trim().ifBlank { url.substringAfterLast('/') }
            ParsedChapter(name, url)
        }.distinctBy { it.url }
        return if (config.chaptersNewestFirst) chapters.reversed() else chapters
    }

    /** The chapter text as html, without scripts and ads. */
    fun parseContent(doc: Document, config: CustomProviderConfig): String? {
        val content = doc.selectFirst(config.content) ?: return null
        content.select("script, style, iframe, ins, noscript").remove()
        val html = content.html().trim()
        return html.takeIf { it.isNotBlank() }
    }

    private fun imageUrl(image: Element): String? {
        for (attribute in imageAttributes) {
            val url = image.absUrl(attribute)
            if (url.isNotBlank() && !url.startsWith("data:")) return url
        }
        return null
    }
}

enum class TestStep { Search, Novel, Chapter }

/** Outcome of trying a provider: what worked and, when it stopped, where and why. */
data class ProviderTestReport(
    val results: Int = 0,
    val firstName: String? = null,
    val chapters: Int = 0,
    val textLength: Int = 0,
    val failedStep: TestStep? = null,
    val error: String? = null,
) {
    val success get() = failedStep == null
}

/** A provider created by the user from a [CustomProviderConfig]. */
class CustomProvider(val config: CustomProviderConfig) : MainAPI() {
    override val name = config.name
    override val mainUrl = config.baseUrl.trim().trimEnd('/')
    override val lang = config.lang

    /** Downloads [url] and parses it with the url as base, so relative links resolve to absolute ones. */
    private suspend fun fetch(url: String): Document = Jsoup.parse(app.get(url).text, url)

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = fetch(CustomProviderParser.searchUrl(config, query))
        return CustomProviderParser.parseSearch(doc, config).map { result ->
            newSearchResponse(name = result.name, url = result.url) {
                posterUrl = result.poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = fetch(url)
        val novel = CustomProviderParser.parseNovel(doc, config)
            ?: throw ErrorLoadingException("Novel title not found, check the title selector")
        val chapterDoc = if (config.chaptersViaAjax) {
            Jsoup.parse(app.post("${url.trimEnd('/')}/ajax/chapters/").text, url)
        } else {
            doc
        }
        val chapters = CustomProviderParser.parseChapters(chapterDoc, config)
        if (chapters.isEmpty()) {
            throw ErrorLoadingException("No chapters found, check the chapter selector")
        }
        return newStreamResponse(
            name = novel.name,
            url = url,
            data = chapters.map { ChapterData(name = it.name, url = it.url, dateOfRelease = null, views = null) }
        ) {
            author = novel.author
            posterUrl = novel.poster
            synopsis = novel.synopsis
        }
    }

    override suspend fun loadHtml(url: String): String? {
        val doc = fetch(url)
        return CustomProviderParser.parseContent(doc, config)
    }

    /**
     * Searches [query], opens the first novel and reads its first chapter, so the user
     * can see which selector is wrong before saving the provider.
     */
    suspend fun runTest(query: String): ProviderTestReport {
        var report = ProviderTestReport(failedStep = TestStep.Search)
        return try {
            val results = search(query)
            val first = results.firstOrNull() ?: return report.copy(
                error = "No results for \"$query\""
            )
            report = ProviderTestReport(results.size, first.name, failedStep = TestStep.Novel)
            val novel = load(first.url) as? com.lagradost.quicknovel.StreamResponse
                ?: return report.copy(error = "The page is not a novel")
            report = ProviderTestReport(
                results.size, first.name, novel.data.size, failedStep = TestStep.Chapter
            )
            val text = loadHtml(novel.data.first().url)
                ?: return report.copy(error = "Chapter text not found")
            ProviderTestReport(results.size, first.name, novel.data.size, text.length)
        } catch (e: Exception) {
            report.copy(error = e.message ?: e.javaClass.simpleName)
        }
    }
}
