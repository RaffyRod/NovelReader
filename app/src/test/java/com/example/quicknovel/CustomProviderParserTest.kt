package com.example.quicknovel

import com.lagradost.quicknovel.providers.ConfigProblem
import com.lagradost.quicknovel.providers.CustomProviderConfig
import com.lagradost.quicknovel.providers.CustomProviderParser
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomProviderParserTest {
    private val base = "https://site.test"

    private val config = CustomProviderConfig(
        name = "Site",
        baseUrl = base,
        searchUrl = "/search?q={query}",
        resultItem = "li.result",
        resultLink = "a.title",
        resultImage = "img",
        novelTitle = "h1.name",
        novelAuthor = ".author",
        novelCover = "img.cover",
        novelSynopsis = ".summary",
        chapterLink = "ul.chapters a",
        content = "div.text",
    )

    private fun page(html: String) = Jsoup.parse(html, "$base/novel/one/")

    @Test
    fun `search results keep name, absolute url and lazy loaded cover`() {
        val doc = page(
            """
            <ul>
              <li class="result"><a class="title" href="/novel/one">One</a><img data-src="/c/one.jpg" src="data:image/gif;base64,xx"></li>
              <li class="result"><a class="title" href="https://other.test/n/two" title="Two long title">Two</a></li>
              <li class="result"><span>no link</span></li>
            </ul>
            """
        )

        val results = CustomProviderParser.parseSearch(doc, config)

        assertEquals(2, results.size)
        assertEquals("One", results[0].name)
        assertEquals("https://site.test/novel/one", results[0].url)
        assertEquals("https://site.test/c/one.jpg", results[0].poster)
        assertEquals("Two long title", results[1].name)
        assertEquals("https://other.test/n/two", results[1].url)
        assertNull(results[1].poster)
    }

    @Test
    fun `a result that is itself the link is accepted`() {
        val linkConfig = config.copy(resultItem = "a.novel", resultLink = "")
        val doc = page("""<a class="novel" href="/n/x">Title X</a>""")

        val results = CustomProviderParser.parseSearch(doc, linkConfig)

        assertEquals(listOf("Title X"), results.map { it.name })
    }

    @Test
    fun `novel details are read and optional parts may be missing`() {
        val full = page(
            """<h1 class="name"> Big Novel </h1><span class="author">Ann</span>
               <img class="cover" src="/cover.png"><div class="summary">A story.</div>"""
        )
        val minimal = page("""<h1 class="name">Only title</h1>""")

        val novel = CustomProviderParser.parseNovel(full, config)!!
        assertEquals("Big Novel", novel.name)
        assertEquals("Ann", novel.author)
        assertEquals("https://site.test/cover.png", novel.poster)
        assertEquals("A story.", novel.synopsis)

        val small = CustomProviderParser.parseNovel(minimal, config)!!
        assertEquals("Only title", small.name)
        assertNull(small.author)
        assertNull(small.poster)
        assertNull(CustomProviderParser.parseNovel(page("<p>nothing</p>"), config))
    }

    @Test
    fun `chapters come in reading order whatever the site order is`() {
        val html = """<ul class="chapters">
            <li><a href="/c/3">Chapter 3</a></li><li><a href="/c/2">Chapter 2</a></li>
            <li><a href="/c/1">Chapter 1</a></li><li><a href="/c/1">Chapter 1</a></li></ul>"""

        val newestFirst = CustomProviderParser.parseChapters(page(html), config.copy(chaptersNewestFirst = true))
        val oldestFirst = CustomProviderParser.parseChapters(page(html), config)

        assertEquals(listOf("Chapter 1", "Chapter 2", "Chapter 3"), newestFirst.map { it.name })
        assertEquals(listOf("Chapter 3", "Chapter 2", "Chapter 1"), oldestFirst.map { it.name })
        assertEquals("https://site.test/c/1", newestFirst.first().url)
    }

    @Test
    fun `chapter text drops scripts and ads and empty text gives null`() {
        val doc = page("""<div class="text"><p>Hello</p><script>x()</script><ins>ad</ins><p>World</p></div>""")

        val html = CustomProviderParser.parseContent(doc, config)!!

        assertTrue(html.contains("<p>Hello</p>"))
        assertTrue(html.contains("<p>World</p>"))
        assertFalse(html.contains("script"))
        assertFalse(html.contains("ad<"))
        assertNull(CustomProviderParser.parseContent(page("""<div class="text">  </div>"""), config))
        assertNull(CustomProviderParser.parseContent(page("<p>x</p>"), config))
    }

    @Test
    fun `search url encodes the query and accepts relative paths`() {
        assertEquals(
            "https://site.test/search?q=dark+lord%26more",
            CustomProviderParser.searchUrl(config, "dark lord&more")
        )
        assertEquals(
            "https://other.test/find/a",
            CustomProviderParser.searchUrl(config.copy(searchUrl = "https://other.test/find/{query}"), "a")
        )
        assertEquals("https://site.test/x", CustomProviderParser.absolute("https://site.test/", "x"))
    }

    @Test
    fun `madara template is a valid provider once it has a name and a url`() {
        val template = CustomProviderConfig.madaraTemplate("Madara site", "https://madara.test")

        assertNull(CustomProviderParser.validate(template))
        assertTrue(template.chaptersViaAjax)
        assertTrue(template.chaptersNewestFirst)
        assertEquals(CustomProviderConfig.DEFAULT_SITE_SEARCH_URL, template.searchUrl)
        assertFalse(template.searchUrl.contains("wp-manga"))
        assertTrue(template.resultItem.contains("page-item-detail"))
    }

    @Test
    fun `site bridge template never invents wp-manga or madara ajax`() {
        val template = CustomProviderConfig.siteBridgeTemplate("JS site", "https://js.test")
        assertNull(CustomProviderParser.validate(template))
        assertEquals("/?s={query}", template.searchUrl)
        assertFalse(template.searchUrl.contains("wp-manga"))
        assertFalse(template.chaptersViaAjax)
    }

    @Test
    fun `stripWpMangaPostType removes manga query param for fallbacks`() {
        assertEquals(
            "https://site.test/?s=hero",
            CustomProviderParser.stripWpMangaPostType("https://site.test/?s=hero&post_type=wp-manga"),
        )
        assertEquals(
            "https://site.test/",
            CustomProviderParser.stripWpMangaPostType("https://site.test/?post_type=wp-manga"),
        )
        assertEquals(
            "https://site.test/?s=a&order=desc",
            CustomProviderParser.stripWpMangaPostType("https://site.test/?s=a&post_type=wp-manga&order=desc"),
        )
    }

    @Test
    fun `browseUrls Madara search is LN primary then plain s then homepage`() {
        val madara = CustomProviderConfig.madaraTemplate("Madara", "https://site.test")
        val search = CustomProviderParser.browseUrls(madara, "hero")
        assertEquals(
            listOf(
                "https://site.test/?s=hero&post_type=wp-manga",
                "https://site.test/?s=hero",
                "https://site.test/",
            ),
            search,
        )
        assertEquals(3, search.size)
        assertEquals("https://site.test/", search.last())
        assertFalse(search.dropLast(1).any { it.trimEnd('/') == "https://site.test" })
    }

    @Test
    fun `browseUrls Madara empty query keeps LN s empty then homepage`() {
        val madara = CustomProviderConfig.madaraTemplate("Madara", "https://site.test")
        val browse = CustomProviderParser.browseUrls(madara, "")
        assertEquals("https://site.test/?s=&post_type=wp-manga", browse[0])
        assertEquals("https://site.test/?s=", browse[1])
        assertEquals("https://site.test/", browse.last())
        assertTrue(browse.distinct().size == browse.size)
    }

    @Test
    fun `browseUrls Madara page 2 uses LN page path before query`() {
        val madara = CustomProviderConfig.madaraTemplate("Madara", "https://site.test")
        val page2 = CustomProviderParser.browseUrls(madara, "hero", page = 2)
        assertEquals("https://site.test/page/2/?s=hero&post_type=wp-manga", page2[0])
        assertEquals("https://site.test/page/2/?s=hero", page2[1])
        assertEquals("https://site.test/page/2/", page2.last())
    }

    @Test
    fun `browseUrls siteBridge never invents wp-manga and still ends at homepage`() {
        val bridge = CustomProviderConfig.siteBridgeTemplate("Bridge", "https://js.test")
        val search = CustomProviderParser.browseUrls(bridge, "hero")
        assertEquals("https://js.test/?s=hero", search.first())
        assertFalse(search.any { it.contains("wp-manga") })
        assertEquals("https://js.test/", search.last())
    }

    @Test
    fun `isUsableBrowsePage rejects empty bodies and accepts madara cards`() {
        assertFalse(CustomProviderParser.isUsableBrowsePage(Jsoup.parse("<html><body></body></html>")))
        assertFalse(CustomProviderParser.isUsableBrowsePage(Jsoup.parse("<html></html>")))
        val madaraHome = Jsoup.parse(
            """
            <html><body>
              <div class="page-item-detail">
                <div class="item-thumb"><a href="/novel/one" title="One"><img src="/c.jpg"></a></div>
                <div class="post-title"><a href="/novel/one">One</a></div>
              </div>
            </body></html>
            """.trimIndent(),
            "https://site.test/",
        )
        assertTrue(CustomProviderParser.isUsableBrowsePage(madaraHome))
        val madara = CustomProviderConfig.madaraTemplate("Madara", "https://site.test")
        val parsed = CustomProviderParser.parseSearch(madaraHome, madara)
        assertEquals(1, parsed.size)
        assertEquals("One", parsed[0].name)
    }

    @Test
    fun `migrateBrowseConfig strips stored wp-manga and expands madara homepage selectors`() {
        val plain = CustomProviderConfig.madaraTemplate("Old", "https://old.test").copy(
            searchUrl = "/?s={query}&post_type=wp-manga",
            resultItem = "div.c-tabs-item__content",
            resultLink = "div.post-title a",
        )
        val fixed = CustomProviderConfig.migrateBrowseConfig(plain)
        assertEquals(CustomProviderConfig.DEFAULT_SITE_SEARCH_URL, fixed.searchUrl)
        assertFalse(fixed.searchUrl.contains("wp-manga"))
        assertTrue(fixed.resultItem.contains("page-item-detail"))
        assertTrue(fixed.resultLink.contains("item-thumb"))
    }

    @Test
    fun `migrateBrowseConfig never keeps wp-manga on Madara or bridge`() {
        val madara = CustomProviderConfig.migrateBrowseConfig(
            CustomProviderConfig.madaraTemplate("M", "https://m.test"),
        )
        assertEquals(CustomProviderConfig.DEFAULT_SITE_SEARCH_URL, madara.searchUrl)

        val bridgeWrong = CustomProviderConfig.siteBridgeTemplate("B", "https://b.test").copy(
            searchUrl = "/?s={query}&post_type=wp-manga",
        )
        val fixedBridge = CustomProviderConfig.migrateBrowseConfig(bridgeWrong)
        assertEquals(CustomProviderConfig.DEFAULT_SITE_SEARCH_URL, fixedBridge.searchUrl)
        assertFalse(fixedBridge.searchUrl.contains("wp-manga"))
    }

    @Test
    fun `CSS candidates try siteBridge before Madara so new sites are not wp-manga by default`() {
        val candidates = com.lagradost.quicknovel.providers.CustomProviderPresets.candidates(
            "Asianfanfics",
            "https://www.asianfanfics.com",
            "en",
            com.lagradost.quicknovel.providers.CustomProviderConfig.KIND_CSS,
        )
        assertTrue(candidates.isNotEmpty())
        assertEquals(CustomProviderConfig.DEFAULT_SITE_SEARCH_URL, candidates.first().searchUrl)
        assertFalse(candidates.first().chaptersViaAjax)
        assertFalse(candidates.any { it.searchUrl.contains("wp-manga") })
        assertTrue(candidates.any { it.chaptersViaAjax })
    }

    @Test
    fun `invalid configurations are reported with the first problem`() {
        fun problem(config: CustomProviderConfig, taken: Set<String> = emptySet()) =
            CustomProviderParser.validate(config, taken)?.problem

        assertNull(problem(config))
        assertEquals(ConfigProblem.EmptyName, problem(config.copy(name = " ")))
        assertEquals(ConfigProblem.EmptyName, problem(config, taken = setOf("Site")))
        assertEquals(ConfigProblem.InvalidUrl, problem(config.copy(baseUrl = "site.test")))
        assertEquals(ConfigProblem.InvalidUrl, problem(config.copy(baseUrl = "ftp://site.test")))
        assertEquals(ConfigProblem.SearchUrlWithoutQuery, problem(config.copy(searchUrl = "/search")))
        assertEquals(ConfigProblem.MissingSelector, problem(config.copy(content = "")))
        assertEquals(ConfigProblem.InvalidSelector, problem(config.copy(chapterLink = "ul >> a[")))
    }
}
