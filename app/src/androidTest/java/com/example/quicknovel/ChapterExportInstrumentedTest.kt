package com.lagradost.quicknovel

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

@RunWith(AndroidJUnit4::class)
class ChapterExportInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun chapter(number: Int) = LoadedChapter(
        title = "Title $number",
        html = "<p>Body of chapter $number</p>"
    )

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val entries = LinkedHashMap<String, String>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                entries[entry.name] = zip.readBytes().decodeToString()
            }
        }
        return entries
    }

    private fun epubOf(vararg numbers: Int): Map<String, String> {
        val output = ByteArrayOutputStream()
        BookDownloader2Helper.writeEpubOfChapters(
            context,
            "TestApi",
            "Test Book",
            "Test Author",
            "Synopsis",
            numbers.map { it to chapter(it) },
            output
        )
        return unzip(output.toByteArray())
    }

    @Test
    fun writeEpubOfChapters_nonContiguousChapters_keepsOnlyThoseWithRealNumbers() {
        val entries = epubOf(9, 1)

        assertTrue(entries.keys.any { it.endsWith("chapter1.html") })
        assertTrue(entries.keys.any { it.endsWith("chapter9.html") })
        assertFalse(entries.keys.any { it.endsWith("chapter2.html") })

        val toc = entries.entries.first { it.key.endsWith("toc.ncx") }.value
        assertTrue(toc.contains("1. Title 1"))
        assertTrue(toc.contains("9. Title 9"))
        assertTrue(toc.indexOf("1. Title 1") < toc.indexOf("9. Title 9"))
    }

    @Test
    fun writeEpubOfChapters_metadataTitle_includesTheChapterRange() {
        val entries = epubOf(1, 2, 3, 9)

        val opf = entries.entries.first { it.key.endsWith("content.opf") }.value
        assertTrue(opf.contains("Test Book [1-3, 9]"))
    }

    @Test
    fun writeEpubOfChapters_titleAlreadyNumbered_isNotNumberedTwice() {
        val output = ByteArrayOutputStream()
        BookDownloader2Helper.writeEpubOfChapters(
            context, "TestApi", "Test Book", null, null,
            listOf(3 to LoadedChapter("3. The Bitter Truth", "<p>text</p>")),
            output
        )

        val toc = unzip(output.toByteArray()).entries.first { it.key.endsWith("toc.ncx") }.value
        assertTrue(toc.contains("3. The Bitter Truth"))
        assertFalse(toc.contains("3. 3. The Bitter Truth"))
    }

    @Test(expected = ErrorLoadingException::class)
    fun writeEpubOfChapters_noChapters_throws() {
        BookDownloader2Helper.writeEpubOfChapters(
            context, "TestApi", "Test Book", null, null, emptyList(), ByteArrayOutputStream()
        )
    }

    @Test
    fun write_pdfChapters_producesAValidPdfThatGrowsWithContent() {
        fun pdf(count: Int): ByteArray {
            val output = ByteArrayOutputStream()
            ChapterPdfExporter.write(
                (1..count).map { number ->
                    PdfChapter(
                        ChapterSpec.numberedTitle(number, "Title $number"),
                        listOf("Body of chapter $number")
                    )
                },
                output
            )
            return output.toByteArray()
        }

        val single = pdf(1)
        val several = pdf(3)

        assertNotNull(single)
        assertEquals("%PDF", single.copyOfRange(0, 4).decodeToString())
        assertTrue(several.size > single.size)
    }
}
