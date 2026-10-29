package com.lagradost.quicknovel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterPdfExporterTest {
    @Test
    fun htmlToParagraphs_blockElements_becomeOneParagraphEach() {
        val html = "<p>First   paragraph</p>\n<p>Second paragraph</p><h2>Heading</h2>"

        assertEquals(
            listOf("First paragraph", "Second paragraph", "Heading"),
            ChapterPdfExporter.htmlToParagraphs(html)
        )
    }

    @Test
    fun htmlToParagraphs_lineBreaks_areKeptInsideTheParagraph() {
        val html = "<p>line one<br>line two<br/>line three</p>"

        assertEquals(
            listOf("line one\nline two\nline three"),
            ChapterPdfExporter.htmlToParagraphs(html)
        )
    }

    @Test
    fun htmlToParagraphs_nestedBlocks_areNotDuplicated() {
        val html = "<div><p>Inner text</p></div><ul><li>Item</li></ul>"

        assertEquals(listOf("Inner text", "Item"), ChapterPdfExporter.htmlToParagraphs(html))
    }

    @Test
    fun htmlToParagraphs_scriptsAndStyles_areRemoved() {
        val html = "<style>p{color:red}</style><script>alert(1)</script><p>Visible</p>"

        assertEquals(listOf("Visible"), ChapterPdfExporter.htmlToParagraphs(html))
    }

    @Test
    fun htmlToParagraphs_withoutBlocks_fallsBackToBodyText() {
        assertEquals(listOf("Just plain text"), ChapterPdfExporter.htmlToParagraphs("Just plain text"))
    }

    @Test
    fun htmlToParagraphs_blankContent_returnsNoParagraphs() {
        assertTrue(ChapterPdfExporter.htmlToParagraphs("<p>   </p><p></p>").isEmpty())
    }
}
