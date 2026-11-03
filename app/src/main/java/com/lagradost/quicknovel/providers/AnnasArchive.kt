package com.lagradost.quicknovel.providers

import com.lagradost.quicknovel.DownloadExtractLink
import com.lagradost.quicknovel.DownloadLink
import com.lagradost.quicknovel.DownloadLinkType
import com.lagradost.quicknovel.ErrorLoadingException
import com.lagradost.quicknovel.HeadMainPageResponse
import com.lagradost.quicknovel.LoadResponse
import com.lagradost.quicknovel.MainAPI
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.SearchResponse
import com.lagradost.quicknovel.fixUrlNull
import com.lagradost.quicknovel.network.WebViewResolver
import com.lagradost.quicknovel.network.utils.CookiesUtils
import com.lagradost.quicknovel.newEpubResponse
import com.lagradost.quicknovel.newSearchResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class AnnasArchive : MainAPI() {
    override val lang = "en"
    override val name = "Annas Archive"
    override val mainUrl = MIRRORS.first()
    override val hasMainPage = true
    override val iconId = R.drawable.icon_annasarchive
    override val iconBackgroundId = R.color.white
    override val hasReviews = false

    override val tags = listOf(
        "Fiction" to "book_fiction",
        "Non-fiction" to "book_nonfiction",
        "Unknown" to "book_unknown",
        "Magazine" to "magazine",
        "Comic" to "book_comic",
        "Standards" to "standards_document",
    )
    override val mainCategories = listOf(
        "English" to "en",
        "Spanish" to "es",
        "French" to "fr",
        "German" to "de",
        "Italian" to "it",
        "Portuguese" to "pt",
        "Chinese" to "zh",
        "Japanese" to "ja",
    )
    override val orderBys = listOf(
        "Z-Library" to "zlib",
        "Libgen (li)" to "lgli",
        "Libgen (rs)" to "lgrs",
        "Internet Archive" to "ia",
        "HathiTrust" to "hathi",
        "Uploads" to "upload",
        "Duxiu" to "duxiu",
        "Sci-Hub" to "scihub",
        "MagzDB" to "magzdb"
    )

    override suspend fun loadMainPage(
        page: Int,
        mainCategory: String?,
        orderBy: String?,
        tag: String?
    ): HeadMainPageResponse {
        var lastError: String? = null
        for (base in MIRRORS) {
            val url = searchUrl(
                base = base,
                page = page,
                query = null,
                lang = mainCategory,
                content = tag,
                src = orderBy,
            )
            val cards = fetchCards(url)
            if (cards.isNotEmpty()) return HeadMainPageResponse(url, cards)
            lastError = "empty after okhttp+webview on $base"
        }
        throw ErrorLoadingException(
            lastError ?: "Annas Archive blocked or returned no books",
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        var lastError: String? = null
        for (base in MIRRORS) {
            val url = searchUrl(base = base, page = 1, query = query)
            val cards = fetchCards(url)
            if (cards.isNotEmpty()) return cards
            lastError = "empty search on $base"
        }
        throw ErrorLoadingException(lastError ?: "Annas Archive search returned no books")
    }

    /**
     * OkHttp first (uses any cookies from a prior WebView bypass). On DDoS-Guard /
     * empty HTML, solve in WebView (always finishes), then re-parse and retry OkHttp.
     */
    private suspend fun fetchCards(url: String): List<SearchResponse> {
        val cookies = CookiesUtils.getAllCookiesForUrl(url)
        val first = runCatching {
            app.get(url, cookies = cookies).document
        }.getOrElse { Jsoup.parse("") }
        parseArchiveCards(first).takeIf { it.isNotEmpty() }?.let { return it }

        val html = resolveListHtml(url) ?: return emptyList()
        parseArchiveCards(Jsoup.parse(html, url)).takeIf { it.isNotEmpty() }?.let { return it }

        // Challenge cookies may now be in CookieManager — one more OkHttp pass.
        val after = runCatching {
            app.get(url, cookies = CookiesUtils.getAllCookiesForUrl(url)).document
        }.getOrElse { Jsoup.parse("") }
        return parseArchiveCards(after)
    }

    private suspend fun resolveListHtml(url: String): String? {
        // Always call NativeAndroid so we never hang on the 60s WebView timeout.
        val script = """
            (function() {
                function finish(html) {
                    if (window.__aaFinished) return;
                    window.__aaFinished = true;
                    try { NativeAndroid.onElementFound(html || document.documentElement.outerHTML); } catch (e) {}
                }
                var ticks = 0;
                var checkInterval = setInterval(function() {
                    ticks++;
                    var challenge = document.querySelector('[data-ddg-origin], #ddg-l10n-title, .ddos-guard');
                    var list = document.querySelector("div.js-aarecord-list-outer");
                    if (list && list.innerText && list.innerText.trim().length > 0) {
                        clearInterval(checkInterval);
                        finish(list.outerHTML);
                        return;
                    }
                    if (!challenge && document.body && document.body.innerText.trim().length > 400) {
                        var md5 = document.querySelector('a[href*="/md5/"]');
                        if (md5) {
                            clearInterval(checkInterval);
                            finish(document.documentElement.outerHTML);
                            return;
                        }
                    }
                    if (ticks >= 22) {
                        clearInterval(checkInterval);
                        finish(document.documentElement.outerHTML);
                    }
                }, 1000);
                setTimeout(function() {
                    clearInterval(checkInterval);
                    finish(document.documentElement.outerHTML);
                }, 24000);
            })();
        """.trimIndent()
        return WebViewResolver(scriptToFinish = script, useOkhttp = false)
            .resolveUsingWebView(url)
            ?.takeIf { it.isNotBlank() && "DDoS-Guard" !in it }
    }

    private fun searchUrl(
        base: String,
        page: Int,
        query: String?,
        lang: String? = null,
        content: String? = null,
        src: String? = null,
    ): String = buildString {
        append(base.trimEnd('/'))
        append("/search?index=&page=$page&display=&ext=epub")
        if (!query.isNullOrBlank()) append("&q=${query.trim().replace(" ", "+")}")
        if (!lang.isNullOrBlank()) append("&lang=$lang")
        if (!content.isNullOrBlank()) append("&content=$content")
        if (!src.isNullOrBlank()) append("&src=$src")
    }

    private fun extract(url: String, name: String, force: Boolean = false): DownloadLinkType {
        return if (url.contains(".epub") || force) {
            DownloadLink(url = url, name = name, kbPerSec = 2)
        } else {
            DownloadExtractLink(url = url, name = name)
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url, cookies = CookiesUtils.getAllCookiesForUrl(mainUrl)).document
        var slowLink: String? = null

        return newEpubResponse(
            name = document.selectFirst("div.text-2xl")?.ownText()
                ?: throw ErrorLoadingException("No title found"),
            url = url,
            links = document.select("ul.mb-4 > li").mapNotNull { element ->
                val link = fixUrlNull(element.selectFirst("a.js-download-link")?.attr("href"))
                    ?: return@mapNotNull null

                if (link.contains("fast_download")) return@mapNotNull null

                if (element.text().contains("no waitlist")) {
                    if (slowLink == null) {
                        slowLink = getSlowLinkWithWebView(link)
                    }
                    return@mapNotNull if (slowLink != null) {
                        extract(slowLink, element.text(), true)
                    } else {
                        null
                    }
                }

                if (link.endsWith("/datasets")) return@mapNotNull null
                extract(link, element.text())
            },
        ) {
            posterUrl =
                document.selectFirst("main > div > div > div > div > div > img")?.attr("src")
            author = document.selectFirst("main > div > div > a")?.ownText()
            synopsis = document.selectFirst("main > div > div > div > div.mb-1")?.text()
        }
    }

    private suspend fun getSlowLinkWithWebView(url: String): String? {
        val script = """
            (function() {
                var checkInterval = setInterval(function() {
                    var element = document.querySelector("main > div > p.mb-4.text-xs > span > span");
                    if (element && element.innerText.trim().length > 0) {
                        clearInterval(checkInterval);
                        NativeAndroid.onElementFound(element.innerText.trim());
                    }
                }, 1000);
                setTimeout(function() {
                    clearInterval(checkInterval);
                    try { NativeAndroid.onElementFound(""); } catch (e) {}
                }, 30000);
            })();
        """.trimIndent()
        return WebViewResolver(scriptToFinish = script, useOkhttp = false)
            .resolveUsingWebView(url)
            ?.takeIf { it.isNotBlank() }
    }

    /** Cards from full page HTML or WebView fragment of `js-aarecord-list-outer`. */
    fun parseArchiveCards(document: Document): List<SearchResponse> {
        if (document.selectFirst("[data-ddg-origin], #ddg-l10n-title") != null) {
            return emptyList()
        }
        val uncommented = Jsoup.parse(
            document.html().replace(Regex("<!--([\\W\\w]*?)-->")) { it.groupValues[1] },
        )
        val nodes = uncommented.select("div.js-aarecord-list-outer > div")
            .ifEmpty { uncommented.select("div.js-aarecord-list-outer div.flex.pt-3.pb-3") }
            .ifEmpty { uncommented.select("div.flex.pt-3.pb-3.border-b") }
            .ifEmpty { uncommented.select("a[href*=/md5/]") }
        return nodes.mapNotNull { node ->
            val a = when {
                node.tagName() == "a" && node.hasAttr("href") -> node
                else -> node.selectFirst("a.line-clamp-\\[3\\]")
                    ?: node.selectFirst("a[href*=/md5/]")
            } ?: return@mapNotNull null
            val href = fixUrlNull(a.attr("href")) ?: return@mapNotNull null
            if ("/md5/" !in href) return@mapNotNull null
            val title = a.text().trim().ifBlank { return@mapNotNull null }
            newSearchResponse(name = title, url = href) {
                posterUrl = fixUrlNull(
                    (if (node.tagName() == "a") node.parent() else node)
                        ?.selectFirst("img")
                        ?.attr("src"),
                )
            }
        }.distinctBy { it.url }
    }

    companion object {
        /** Official mirrors (FAQ). Tried in order when DDoS-Guard empties a host. */
        val MIRRORS = listOf(
            "https://annas-archive.gl",
            "https://annas-archive.pk",
            "https://annas-archive.gd",
        )
    }
}
