package com.example.quicknovel

import com.lagradost.quicknovel.providers.BloggerParser
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BloggerIndexTest {
    private val base = "https://x.blogspot.com"

    private val home = Jsoup.parse(
        """<a href="$base/p/lista-de-novelas.html">Lista de Novelas</a><a href="$base/p/contacto.html">Contacto</a>""",
        base
    )

    private val index = Jsoup.parse(
        """<div class="post-body">
          <a href="$base/p/lista-de-novelas.html">Lista</a>
          <a href="$base/p/nano.html"><img src="https://img/a/w205-h127/n.jpg"/></a>
          <a href="$base/p/nano.html"><b>13. Nano Machine Novela</b></a>
          <a href="$base/p/descenso.html">14. Nano Machine 2:</a>&nbsp;<a href="$base/p/descenso.html">Descenso Novela</a>
          <a href="$base/p/regresion.html">54. Regresión Absoluta Novela Español</a>
          <a href="$base/2024/10/post.html">not a novel</a>
        </div>""",
        "$base/p/lista-de-novelas.html"
    )

    private val novel = Jsoup.parse(
        """<h3 class="post-title">Regresión Absoluta Novela Español</h3>
        <div class="post-body"><img src="https://img/b/s72-c/c.jpg"/>
          <b>Sinopsis:</b> Volver al pasado. <b>TODOS LOS VOLÚMENES</b>
          <a href="$base/2024/10/rn-capitulo-1.html">1</a>&nbsp;-&nbsp;<a href="$base/2024/10/rn-capitulo-2.html">2</a>
          <a href="$base/2024/10/rn-capitulo-3-4.html">3 - 4</a>
          <a href="$base/2024/10/rn-capitulo-3-4.html">3 - 4</a>
          <a href="$base/p/lista-de-novelas.html">Lista</a>
        </div>""",
        "$base/p/regresion.html"
    )

    @Test
    fun `finds the novel list page from the home links`() {
        assertEquals("$base/p/lista-de-novelas.html", BloggerParser.findIndexUrl(home))
    }

    @Test
    fun `home without a list link has no index`() {
        assertNull(BloggerParser.findIndexUrl(Jsoup.parse("""<a href="$base/p/contacto.html">Contacto</a>""", base)))
    }

    @Test
    fun `index lists one novel per linked page without numbers or novel tag`() {
        val novels = BloggerParser.indexNovels(index, "$base/p/lista-de-novelas.html")
        assertEquals(listOf("Nano Machine", "Nano Machine 2: Descenso", "Regresión Absoluta"), novels.map { it.name })
        assertEquals("https://img/a/s400/n.jpg", novels.first().poster)
        assertNull(novels.last().poster)
    }

    @Test
    fun `novel page gives title synopsis cover and ordered unique chapters`() {
        val page = BloggerParser.novelPage(novel)
        assertNotNull(page)
        checkNotNull(page)
        assertEquals("Regresión Absoluta", page.name)
        assertEquals("Volver al pasado.", page.synopsis)
        assertEquals("https://img/b/s400/c.jpg", page.cover)
        assertEquals(listOf("Capítulo 1", "Capítulo 2", "Capítulo 3 - 4"), page.chapters.map { it.name })
        assertTrue(page.chapters.all { it.url.contains("/2024/10/") })
    }

    @Test
    fun `page without chapter links is not a novel page`() {
        assertNull(BloggerParser.novelPage(Jsoup.parse("""<div class="post-body">hola</div>""", base)))
    }

    @Test
    fun `blogspot url is treated as blogger`() {
        assertTrue(BloggerParser.looksLikeBlogger(Jsoup.parse("<html/>"), "https://x.blogspot.com"))
    }

    @Test
    fun `home with blogger markers is treated as blogger`() {
        val doc = Jsoup.parse("""<div id="Blog1" class="blog-posts"><div class="post-body">x</div></div>""", "https://ejemplo.com")
        assertTrue(BloggerParser.looksLikeBlogger(doc, "https://ejemplo.com"))
    }

    @Test
    fun `plain site without blogger markers is rejected`() {
        val doc = Jsoup.parse("""<div class="content">hola</div>""", "https://ejemplo.com")
        assertNull(BloggerParser.findIndexUrl(doc))
        assertTrue(!BloggerParser.looksLikeBlogger(doc, "https://ejemplo.com"))
    }
}
