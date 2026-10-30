package com.example.quicknovel

import com.lagradost.quicknovel.providers.BloggerParser
import com.lagradost.quicknovel.providers.BloggerPost
import com.lagradost.quicknovel.providers.CustomProviderConfig
import com.lagradost.quicknovel.providers.CustomProviderParser
import com.lagradost.quicknovel.providers.CustomProviderPresets
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BloggerParserTest {
    private val feed = """
        {"feed":{"entry":[
          {"title":{"${'$'}t":"A Returner's Magic Should Be Special Novel Chapter 91"},
           "link":[{"rel":"replies","href":"https://x.blogspot.com/r"},{"rel":"alternate","href":"https://x.blogspot.com/c91.html"}],
           "content":{"${'$'}t":"<img src=\"https://img.test/cover.jpg\"/><p>text</p>"}},
          {"title":{"${'$'}t":"A Returner's Magic Should Be Special Novel Chapter 9"},
           "link":[{"rel":"alternate","href":"https://x.blogspot.com/c9.html"}],
           "content":{"${'$'}t":"<p>text</p>"}},
          {"title":{"${'$'}t":"Lista de Novelas"},
           "link":[{"rel":"alternate","href":"https://x.blogspot.com/p/lista.html"}],
           "content":{"${'$'}t":"<p>list</p>"}},
          {"title":{"${'$'}t":"Otra Novela - Capítulo 2"},
           "link":[{"rel":"alternate","href":"https://x.blogspot.com/o2.html"}],
           "content":{"${'$'}t":"<p>text</p>"}}
        ]}}
    """.trimIndent()

    private val posts = BloggerParser.parseFeed(feed)

    @Test
    fun `the feed gives title and alternate link`() {
        assertEquals(4, posts.size)
        assertEquals("https://x.blogspot.com/c91.html", posts.first().url)
    }

    @Test
    fun `chapter titles are split into novel and number`() {
        val chapter = BloggerParser.chapterOf(posts[0])
        assertEquals("A Returner's Magic Should Be Special Novel", chapter?.novel)
        assertEquals(91.0, chapter?.number ?: 0.0, 0.0)
        assertEquals("Otra Novela", BloggerParser.chapterOf(posts[3])?.novel)
    }

    @Test
    fun `pages that are not chapters are ignored`() {
        assertNull(BloggerParser.chapterOf(posts[2]))
    }

    @Test
    fun `novels are listed once each`() {
        assertEquals(
            listOf("A Returner's Magic Should Be Special Novel", "Otra Novela"),
            BloggerParser.novels(posts).map { it.first }
        )
    }

    @Test
    fun `chapters come in numeric order`() {
        val chapters = BloggerParser.chapters(posts, "a returner's magic should be special novel")
        assertEquals(listOf(9.0, 91.0), chapters.map { it.number })
    }

    @Test
    fun `the cover is the first image of the post`() {
        assertEquals("https://img.test/cover.jpg", BloggerParser.cover(posts[0]))
        assertNull(BloggerParser.cover(posts[1]))
    }

    @Test
    fun `chapter text is taken from the post body`() {
        val doc = Jsoup.parse("<div class='post-body'><p>Hola</p><script>x()</script></div>")
        val html = BloggerParser.parseContent(doc, "div.post-body")
        assertNotNull(html)
        assertTrue(html!!.contains("Hola"))
        assertTrue(!html.contains("script"))
    }

    @Test
    fun `blogspot addresses try the blogger layout first`() {
        val first = CustomProviderPresets.candidates("Ani", "https://x.blogspot.com", "es").first()
        assertEquals(CustomProviderConfig.KIND_BLOGGER, first.kind)
        assertNull(CustomProviderParser.validate(first))
    }

    @Test
    fun `other addresses keep blogger as the last option`() {
        val last = CustomProviderPresets.candidates("Ani", "https://misitio.com", "es").last()
        assertEquals(CustomProviderConfig.KIND_BLOGGER, last.kind)
    }
}
