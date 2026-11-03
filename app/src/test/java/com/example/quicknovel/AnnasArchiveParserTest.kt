package com.example.quicknovel

import com.lagradost.quicknovel.providers.AnnasArchive
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnasArchiveParserTest {
    private val api = AnnasArchive()

    @Test
    fun `ddos guard html yields no cards`() {
        val doc = Jsoup.parse(
            """
            <html><head><title>DDoS-Guard</title></head>
            <body data-ddg-origin="true"><h1 id="ddg-l10n-title">Checking</h1></body></html>
            """.trimIndent(),
        )
        assertTrue(api.parseArchiveCards(doc).isEmpty())
    }

    @Test
    fun `list outer cards parse title url and cover`() {
        val doc = Jsoup.parse(
            """
            <html><body>
              <div class="js-aarecord-list-outer">
                <div class="flex pt-3 pb-3 border-b">
                  <img src="/cover1.jpg"/>
                  <a class="line-clamp-[3]" href="/md5/abc123">Mother of Learning</a>
                </div>
                <div class="flex pt-3 pb-3 border-b">
                  <a href="/md5/def456">Second Book</a>
                </div>
                <div class="flex pt-3 pb-3 border-b">
                  <a href="/datasets">skip me</a>
                </div>
              </div>
            </body></html>
            """.trimIndent(),
            "https://annas-archive.gl/",
        )
        val cards = api.parseArchiveCards(doc)
        assertEquals(2, cards.size)
        assertEquals("Mother of Learning", cards[0].name)
        assertTrue(cards[0].url.contains("/md5/abc123"))
        assertEquals("Second Book", cards[1].name)
    }

    @Test
    fun `mirrors list matches official FAQ hosts`() {
        assertEquals(
            listOf(
                "https://annas-archive.gl",
                "https://annas-archive.pk",
                "https://annas-archive.gd",
            ),
            AnnasArchive.MIRRORS,
        )
    }
}
