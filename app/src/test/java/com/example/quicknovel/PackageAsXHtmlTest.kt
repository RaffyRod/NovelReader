package com.example.quicknovel

import com.lagradost.quicknovel.packageAsXHtml
import com.lagradost.quicknovel.sanitizeChapterHtmlForEpub
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageAsXHtmlTest {
    @Test
    fun escapesTitleSpecialCharacters() {
        val xhtml = packageAsXHtml("Capítulo 1: A & B <C>", "<p>Hola</p>", "Capítulo 1: A & B <C>")
        assertTrue(xhtml.contains("&amp;"))
        assertTrue(xhtml.contains("&lt;"))
        assertFalse(xhtml.contains("<title>Capítulo 1: A & B <C></title>"))
    }

    @Test
    fun sanitizesNestedHtmlFromTranslator() {
        val dirty = "<html><head></head><body><p>Uno & dos</p><script>x()</script></body></html>"
        val clean = sanitizeChapterHtmlForEpub(dirty)
        assertTrue(clean.contains("Uno"))
        assertTrue(clean.contains("&amp;") || clean.contains("dos"))
        assertFalse(clean.contains("<script"))
        assertFalse(clean.contains("<html"))
    }
}
