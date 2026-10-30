package com.lagradost.quicknovel.providers

import com.lagradost.quicknovel.ChapterData
import com.lagradost.quicknovel.ErrorLoadingException
import com.lagradost.quicknovel.HeadMainPageResponse
import com.lagradost.quicknovel.LoadResponse
import com.lagradost.quicknovel.MainAPI
import com.lagradost.quicknovel.SearchResponse
import com.lagradost.quicknovel.newSearchResponse
import com.lagradost.quicknovel.newStreamResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import com.fasterxml.jackson.databind.ObjectMapper

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
    /** [KIND_CSS] reads pages with the selectors above, [KIND_BLOGGER] reads the feed of a Blogger site. */
    val kind: String = KIND_CSS,
) {
    val isBlogger get() = kind == KIND_BLOGGER

    companion object {
        const val KIND_CSS = "css"
        const val KIND_BLOGGER = "blogger"

        /** Blogger sites publish every chapter as a post; novels are rebuilt from the post titles. */
        fun bloggerTemplate(name: String = "", baseUrl: String = "") = CustomProviderConfig(
            name = name,
            baseUrl = baseUrl,
            searchUrl = "/feeds/posts/default?alt=json&q={query}&max-results=150",
            resultItem = "h3.post-title",
            resultLink = "a",
            novelTitle = "h3.post-title",
            chapterLink = "h3.post-title a",
            content = "div.post-body",
            kind = KIND_BLOGGER,
        )

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

data class BloggerPost(val title: String, val url: String, val html: String, val thumbnail: String? = null)
data class BloggerNovel(val name: String, val url: String, val poster: String?)
data class BloggerNovelPage(val name: String, val synopsis: String?, val cover: String?, val chapters: List<ParsedChapter>)
data class BloggerChapter(val novel: String, val number: Double, val post: BloggerPost)

/** Reads the JSON feed of a Blogger site and groups its posts into novels, with no network access. */
object BloggerParser {
    private val mapper = ObjectMapper()
    private val chapterTitle = Regex(
        """^(.*)\b(?:chapter|chap|cap[ií]tulo|cap|episode|episodio|ep|ch)\.?\s*(\d+(?:\.\d+)?)\b.*$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    /** Labels some sites add to every title, like "(Novela)". */
    private val typeSuffix = Regex("""\(\s*(?:novela|novel|web novel|light novel)\s*\)\s*$""", RegexOption.IGNORE_CASE)

    fun parseFeed(json: String): List<BloggerPost> {
        val entries = mapper.readTree(json).path("feed").path("entry")
        return entries.mapNotNull { entry ->
            val title = entry.path("title").path("\$t").asText("").trim()
            val url = entry.path("link").firstOrNull { it.path("rel").asText() == "alternate" }
                ?.path("href")?.asText("").orEmpty()
            val html = entry.path("content").path("\$t").asText(entry.path("summary").path("\$t").asText(""))
            val thumbnail = entry.path("media\$thumbnail").path("url").asText("")
                .takeIf { it.startsWith("http") }
                ?.replace(Regex("/s\\d+(-c)?/"), "/s400/")
            if (title.isBlank() || url.isBlank()) null else BloggerPost(title, url, html, thumbnail)
        }
    }

    /** Splits "My Novel Chapter 12" into the novel name and the chapter number, null for pages that are not chapters. */
    fun chapterOf(post: BloggerPost): BloggerChapter? {
        val match = chapterTitle.matchEntire(post.title) ?: return null
        val novel = match.groupValues[1].trim().trimEnd('-', '–', '—', ':', '|', ',', '.').trim()
            .replace(typeSuffix, "").trim().trimEnd('-', '–', '—', ':', '|', ',', '.').trim()
        val number = match.groupValues[2].toDoubleOrNull() ?: return null
        if (novel.isBlank()) return null
        return BloggerChapter(novel, number, post)
    }

    /** Novels found in [posts], keeping the order of the feed. */
    fun novels(posts: List<BloggerPost>): List<Pair<String, BloggerPost>> =
        posts.mapNotNull { chapterOf(it) }
            .distinctBy { it.novel.lowercase() }
            .map { it.novel to it.post }

    /** Chapters of [novel] in reading order. */
    fun chapters(posts: List<BloggerPost>, novel: String): List<BloggerChapter> =
        posts.mapNotNull { chapterOf(it) }
            .filter { it.novel.equals(novel, ignoreCase = true) }
            .distinctBy { it.post.url }
            .sortedBy { it.number }

    private val staticPage = Regex("""/p/[^/?#]+\.html""")
    private val datedPost = Regex("""/\d{4}/\d{2}/[^/?#]+\.html""")
    private val indexLabel = Regex(
        """(lista|[íi]ndice|cat[áa]logo|directorio)\s+de\s+(novelas|libros|series)|novel\s*list|todas\s+las\s+novelas""",
        RegexOption.IGNORE_CASE
    )

    /** Page that lists every novel of a blog, found through a link such as "Lista de Novelas". */
    fun findIndexUrl(home: Document): String? =
        home.select("a[href]").firstOrNull {
            indexLabel.containsMatchIn(it.text()) && staticPage.containsMatchIn(it.absUrl("href"))
        }?.absUrl("href")?.substringBefore('#')?.substringBefore('?')

    /** "14. Mi Novela Español" becomes "Mi Novela"-like names without the list number or the "Novela" tag. */
    fun cleanNovelName(raw: String): String =
        raw.replace('\u00a0', ' ')
            .replace(Regex("^\\s*\\d+\\s*[.)\\-]\\s*"), "")
            .replace(Regex("\\s*\\bNovela(\\s+(en\\s+)?Espa[ñn]ol)?\\s*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun bigImage(url: String?): String? =
        url?.takeIf { it.startsWith("http") }?.replace(Regex("/(w\\d+-h\\d+|s\\d+)(-c)?/"), "/s400/")

    /** Novels listed on the index page, one entry per linked novel page, with the cover shown next to it. */
    fun indexNovels(doc: Document, indexUrl: String): List<BloggerNovel> {
        val body = doc.selectFirst(".post-body, .entry-content") ?: doc.body()
        val groups = LinkedHashMap<String, MutableList<Element>>()
        body.select("a[href]").forEach { link ->
            val href = link.absUrl("href").substringBefore('#').substringBefore('?')
            if (!staticPage.containsMatchIn(href) || href == indexUrl.substringBefore('?')) return@forEach
            groups.getOrPut(href) { mutableListOf() }.add(link)
        }
        return groups.mapNotNull { (href, links) ->
            val name = cleanNovelName(links.map { it.text() }.filter { it.isNotBlank() }.joinToString(" "))
                .takeIf { it.isNotBlank() && !it.startsWith("Lista de", ignoreCase = true) } ?: return@mapNotNull null
            val poster = links.firstNotNullOfOrNull { it.selectFirst("img[src]")?.absUrl("src") }
            BloggerNovel(name, href, bigImage(poster))
        }
    }

    /** A novel page: title, synopsis, cover and the links to its chapters in order. */
    fun novelPage(doc: Document): BloggerNovelPage? {
        val body = doc.selectFirst(".post-body, .entry-content") ?: return null
        val chapters = body.select("a[href]").mapNotNull { link ->
            val href = link.absUrl("href").substringBefore('#')
            if (!datedPost.containsMatchIn(href)) return@mapNotNull null
            val text = link.text().replace('\u00a0', ' ').trim()
            val label = if (Regex("^[\\d\\s\\-–.,]+$").matches(text)) "Capítulo $text" else text
            ParsedChapter(label.ifBlank { href.substringAfterLast('/').removeSuffix(".html") }, href)
        }.distinctBy { it.url }
        if (chapters.isEmpty()) return null
        val title = doc.selectFirst("h3.post-title, h1.post-title, .entry-title")?.text()
            ?.let { cleanNovelName(it) }?.takeIf { it.isNotBlank() }
            ?: cleanNovelName(doc.title())
        val text = body.text().replace('\u00a0', ' ')
        val synopsis = Regex("Sinopsis\\s*:?\\s*(.*?)(?:todos los (?:vol[úu]menes|cap[íi]tulos)|$)", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
        val cover = bigImage(body.selectFirst("img[src]")?.absUrl("src"))
        return BloggerNovelPage(title, synopsis, cover, chapters)
    }

    fun isNovelPage(url: String) = staticPage.containsMatchIn(url)

    /** True when the home page or its URL clearly belongs to a Blogger / Blogspot site. */
    fun looksLikeBlogger(home: Document, baseUrl: String): Boolean {
        if (baseUrl.contains("blogspot.", ignoreCase = true) || baseUrl.contains("blogger.com", ignoreCase = true)) {
            return true
        }
        val html = home.html()
        return html.contains("blogger.com", ignoreCase = true) ||
            html.contains("blogspot.", ignoreCase = true) ||
            home.selectFirst("meta[content*=blogger], link[href*=blogger], link[href*=blogspot]") != null ||
            home.selectFirst("#Blog1, .blog-posts, .post-body") != null
    }

    fun cover(post: BloggerPost): String? =
        Jsoup.parse(post.html).selectFirst("img[src]")?.attr("src")?.takeIf { it.startsWith("http") }
            ?: post.thumbnail

    /** The chapter text of a post page, without scripts and the share and comment blocks. */
    fun parseContent(doc: Document, selector: String): String? {
        val body = doc.selectFirst(".post-body, .entry-content, $selector") ?: return null
        body.select("script, style, iframe, ins, noscript, .post-footer, .comments").remove()
        return body.html().trim().takeIf { it.isNotBlank() }
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

/**
 * Result of checking that a Blogger address is reachable and that novels and chapters can be read.
 * [mode] is "index" when a novel list page was used, or "feed" when posts were grouped from the JSON feed.
 */
data class BloggerValidation(
    val ok: Boolean = false,
    val novels: Int = 0,
    val sampleNovel: String? = null,
    val chapters: Int = 0,
    val textLength: Int = 0,
    val mode: String? = null,
    val step: String? = null,
    val error: String? = null,
)

/** A provider created by the user from a [CustomProviderConfig]. */
class CustomProvider(val config: CustomProviderConfig) : MainAPI() {
    override val name = config.name
    override val mainUrl = config.baseUrl.trim().trimEnd('/')
    override val lang = config.lang

    /** Downloads [url] and parses it with the url as base, so relative links resolve to absolute ones. */
    private suspend fun fetch(url: String): Document = Jsoup.parse(app.get(url).text, url)

    override val hasMainPage = true

    /** The provider list shows every site; opening it lists the newest novels, or what an empty search gives. */
    override suspend fun loadMainPage(
        page: Int,
        mainCategory: String?,
        orderBy: String?,
        tag: String?
    ): HeadMainPageResponse {
        if (config.isBlogger) {
            val indexed = bloggerIndexNovels()
            if (indexed.isNotEmpty()) {
                return HeadMainPageResponse(mainUrl, if (page > 1) emptyList() else indexed.map(::indexResponse))
            }
            val url = "$mainUrl/feeds/posts/summary?alt=json&max-results=$BLOGGER_LATEST" +
                "&start-index=${(page - 1) * BLOGGER_LATEST + 1}"
            val posts = BloggerParser.parseFeed(app.get(url).text)
            val list = BloggerParser.novels(posts).map { (novel, post) ->
                newSearchResponse(name = novel, url = bloggerNovelUrl(novel)) {
                    posterUrl = BloggerParser.cover(post)
                }
            }
            return HeadMainPageResponse(mainUrl, list)
        }
        val url = CustomProviderParser.searchUrl(config, "").let {
            if (page > 1) "$it${if (it.contains('?')) "&" else "?"}paged=$page" else it
        }
        val results = CustomProviderParser.parseSearch(fetch(url), config).map { result ->
            newSearchResponse(name = result.name, url = result.url) { posterUrl = result.poster }
        }
        return HeadMainPageResponse(url, results)
    }

    private var bloggerIndex: List<BloggerNovel>? = null

    /** Novels of a blog that keeps a "Lista de Novelas" page; empty when the blog has no such index. */
    private suspend fun bloggerIndexNovels(): List<BloggerNovel> {
        bloggerIndex?.let { return it }
        val indexUrl = BloggerParser.findIndexUrl(fetch(mainUrl))
        val novels = indexUrl?.let { BloggerParser.indexNovels(fetch(it), it) }.orEmpty()
        return (if (novels.size >= MIN_INDEX_NOVELS) novels else emptyList()).also { bloggerIndex = it }
    }

    private fun indexResponse(novel: BloggerNovel) =
        newSearchResponse(name = novel.name, url = novel.url) { posterUrl = novel.poster }

    private fun normalized(text: String) =
        java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").lowercase()

    private suspend fun bloggerPageLoad(url: String): LoadResponse {
        val page = BloggerParser.novelPage(fetch(url))
            ?: throw ErrorLoadingException("No chapters found on the novel page")
        return newStreamResponse(
            name = page.name,
            url = url,
            data = page.chapters.map { ChapterData(name = it.name, url = it.url, dateOfRelease = null, views = null) }
        ) {
            posterUrl = page.cover
            synopsis = page.synopsis
        }
    }

    private fun bloggerNovelUrl(novel: String) = "$mainUrl/search?q=${URLEncoder.encode(novel, "UTF-8")}"

    private suspend fun bloggerFeed(query: String, startIndex: Int = 1, max: Int = 150): List<BloggerPost> {
        val url = "$mainUrl/feeds/posts/default?alt=json&max-results=$max&start-index=$startIndex" +
            "&q=${URLEncoder.encode(query, "UTF-8")}"
        return BloggerParser.parseFeed(app.get(url).text)
    }

    private suspend fun bloggerSearch(query: String): List<SearchResponse> {
        val indexed = bloggerIndexNovels()
        if (indexed.isNotEmpty()) {
            val words = normalized(query).split(" ").filter { it.isNotBlank() }
            return indexed.filter { novel -> normalized(novel.name).let { n -> words.all { it in n } } }
                .map(::indexResponse)
        }
        return BloggerParser.novels(bloggerFeed(query)).map { (novel, post) ->
            newSearchResponse(name = novel, url = bloggerNovelUrl(novel)) {
                posterUrl = BloggerParser.cover(post)
            }
        }
    }

    private suspend fun bloggerLoad(url: String): LoadResponse {
        if (BloggerParser.isNovelPage(url)) return bloggerPageLoad(url)
        val novel = URLDecoder.decode(url.substringAfter("q=", ""), "UTF-8").trim()
        if (novel.isBlank()) throw ErrorLoadingException("Novel name not found in the address")
        val posts = mutableListOf<BloggerPost>()
        var start = 1
        while (start <= 1000) {
            val page = bloggerFeed("\"$novel\"", start, BLOGGER_PAGE)
            posts += page
            if (page.size < BLOGGER_PAGE) break
            start += BLOGGER_PAGE
        }
        val chapters = BloggerParser.chapters(posts, novel)
        if (chapters.isEmpty()) throw ErrorLoadingException("No chapters found for $novel")
        return newStreamResponse(
            name = novel,
            url = url,
            data = chapters.map {
                ChapterData(name = "Chapter ${formatNumber(it.number)}", url = it.post.url, dateOfRelease = null, views = null)
            }
        ) {
            posterUrl = BloggerParser.cover(chapters.first().post)
        }
    }

    private fun formatNumber(number: Double) =
        if (number % 1.0 == 0.0) number.toInt().toString() else number.toString()

    override suspend fun search(query: String): List<SearchResponse> {
        if (config.isBlogger) return bloggerSearch(query)
        val doc = fetch(CustomProviderParser.searchUrl(config, query))
        return CustomProviderParser.parseSearch(doc, config).map { result ->
            newSearchResponse(name = result.name, url = result.url) {
                posterUrl = result.poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        if (config.isBlogger) return bloggerLoad(url)
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
        if (config.isBlogger) return BloggerParser.parseContent(doc, config.content)
        return CustomProviderParser.parseContent(doc, config)
    }

    private companion object {
        const val BLOGGER_PAGE = 150
        const val BLOGGER_LATEST = 500
        const val MIN_INDEX_NOVELS = 3
        const val TEST_NOVEL_TRIES = 5
    }

    /**
     * Checks that this Blogger site answers, looks like Blogger, lists novels (index or feed),
     * and that at least one novel opens with readable chapter text. Used before saving a blog provider.
     */
    suspend fun validateBlogger(): BloggerValidation {
        if (!config.isBlogger) {
            return BloggerValidation(step = "kind", error = "Provider is not a Blogger blog")
        }
        return try {
            val home = fetch(mainUrl)
            if (!BloggerParser.looksLikeBlogger(home, mainUrl)) {
                return BloggerValidation(
                    step = "blogger",
                    error = "The address does not look like a Blogger / Blogspot site"
                )
            }
            val feedUrl = "$mainUrl/feeds/posts/summary?alt=json&max-results=1"
            val feedText = app.get(feedUrl).text
            if (!feedText.contains("\"feed\"") && !feedText.contains("'feed'")) {
                return BloggerValidation(
                    step = "feed",
                    error = "The blog feed did not respond (site down or not Blogger)"
                )
            }
            val indexUrl = BloggerParser.findIndexUrl(home)
            val indexed = indexUrl?.let { BloggerParser.indexNovels(fetch(it), it) }.orEmpty()
            val mode: String
            val results: List<SearchResponse>
            if (indexed.size >= MIN_INDEX_NOVELS) {
                bloggerIndex = indexed
                mode = "index"
                results = indexed.map(::indexResponse)
            } else {
                mode = "feed"
                results = bloggerSearch("")
            }
            if (results.isEmpty()) {
                return BloggerValidation(
                    step = "novels",
                    mode = mode,
                    error = "No novels found on this blog"
                )
            }
            var lastError: String? = null
            for (result in results.take(TEST_NOVEL_TRIES)) {
                val novel = load(result.url) as? com.lagradost.quicknovel.StreamResponse
                if (novel == null || novel.data.isEmpty()) {
                    lastError = "No chapters on ${result.name}"
                    continue
                }
                val text = loadHtml(novel.data.first().url)
                if (text.isNullOrBlank()) {
                    lastError = "Chapter text not found on ${result.name}"
                    continue
                }
                return BloggerValidation(
                    ok = true,
                    novels = results.size,
                    sampleNovel = result.name,
                    chapters = novel.data.size,
                    textLength = text.length,
                    mode = mode,
                )
            }
            BloggerValidation(
                novels = results.size,
                mode = mode,
                step = "chapter",
                error = lastError ?: "Could not read a chapter"
            )
        } catch (e: Exception) {
            BloggerValidation(
                step = "network",
                error = e.message ?: e.javaClass.simpleName
            )
        }
    }

    /**
     * Searches [query], opens the first novel and reads its first chapter, so the user
     * can see which selector is wrong before saving the provider.
     */
    suspend fun runTest(query: String): ProviderTestReport {
        var report = ProviderTestReport(failedStep = TestStep.Search)
        return try {
            var results = search(query)
            if (results.isEmpty() && config.isBlogger && query.isNotBlank()) {
                results = search("")
            }
            val first = results.firstOrNull() ?: return report.copy(
                error = "No results for \"$query\""
            )
            report = ProviderTestReport(results.size, first.name, failedStep = TestStep.Novel)
            var lastError: String? = null
            for (result in results.take(TEST_NOVEL_TRIES)) {
                report = report.copy(firstName = result.name, failedStep = TestStep.Novel)
                val novel = load(result.url) as? com.lagradost.quicknovel.StreamResponse
                if (novel == null || novel.data.isEmpty()) {
                    lastError = "No chapters on ${result.name}"
                    continue
                }
                report = ProviderTestReport(
                    results.size, result.name, novel.data.size, failedStep = TestStep.Chapter
                )
                val text = loadHtml(novel.data.first().url)
                if (text.isNullOrBlank()) {
                    lastError = "Chapter text not found"
                    continue
                }
                return ProviderTestReport(results.size, result.name, novel.data.size, text.length)
            }
            report.copy(error = lastError ?: "The page is not a novel")
        } catch (e: Exception) {
            report.copy(error = e.message ?: e.javaClass.simpleName)
        }
    }
}
