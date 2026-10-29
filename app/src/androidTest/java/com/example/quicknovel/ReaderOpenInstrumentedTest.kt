package com.lagradost.quicknovel

import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReaderOpenInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var pdf: File
    private val novel = "Reader Open Test Novel"

    @Before
    fun createPdf() {
        pdf = File(context.cacheDir, "reader-open-test.pdf")
        val chapters = (1..4).map { number ->
            PdfChapter(
                ChapterSpec.numberedTitle(number, "Title $number"),
                (1..60).map { line -> "Line $line of chapter $number, long enough to fill the page." }
            )
        }
        pdf.outputStream().use { ChapterPdfExporter.write(chapters, it) }
    }

    @After
    fun cleanUp() {
        pdf.delete()
        ExportedFiles.forget(novel, pdf.toUri())
    }

    private fun pageCountOf(file: File): Int =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { it.pageCount }
        }

    private fun waitUntil(timeoutMs: Long = 10_000, condition: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (condition()) return true
            Thread.sleep(100)
        }
        return condition()
    }

    @Test
    fun pdfReader_opensAFile_showsEveryPageAndRendersTheFirstOne() {
        val expectedPages = pageCountOf(pdf)
        assertTrue("The test PDF should have several pages", expectedPages > 1)
        val intent = Intent(context, PdfReaderActivity::class.java).apply {
            setDataAndType(pdf.toUri(), FileHelper.PDF_MIME)
        }

        ActivityScenario.launch<PdfReaderActivity>(intent).use { scenario ->
            var pages = 0
            var indicator = ""
            val rendered = waitUntil {
                scenario.onActivity { activity ->
                    val recycler = activity.findViewById<RecyclerView>(R.id.pdf_pages)
                    pages = recycler.adapter?.itemCount ?: 0
                    indicator = activity.findViewById<TextView>(R.id.pdf_page_indicator).text.toString()
                    renderedFirstPage = (recycler.getChildAt(0) as? ImageView)?.drawable != null
                }
                renderedFirstPage
            }

            assertEquals(expectedPages, pages)
            assertEquals("1 / $expectedPages", indicator)
            assertTrue("The first page was never drawn", rendered)
            holdForInspection()
        }
    }

    @Test
    fun askAndOpen_offersBothReaders_andReadingInTheAppOpensThePdfReader() {
        val intent = Intent(context, PdfReaderActivity::class.java).apply {
            setDataAndType(pdf.toUri(), FileHelper.PDF_MIME)
        }

        ActivityScenario.launch<PdfReaderActivity>(intent).use { scenario ->
            scenario.onActivity { FileOpener.askAndOpen(it, pdf.toUri(), FileHelper.PDF_MIME) }

            onView(withText(R.string.open_in_other_app)).inRoot(isDialog()).check(matches(isDisplayed()))
            holdForInspection()
            onView(withText(R.string.open_in_app)).inRoot(isDialog()).perform(click())

            val opened = waitUntil {
                var count = 0
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    count = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED)
                        .count { it is PdfReaderActivity }
                }
                count == 1 && scenario.state == androidx.lifecycle.Lifecycle.State.CREATED
            }
            assertTrue("A second PDF reader should have opened on top", opened)
        }
    }

    @Test
    fun openInOtherApp_showsTheSystemChooserWithoutCrashing() {
        val intent = Intent(context, PdfReaderActivity::class.java).apply {
            setDataAndType(pdf.toUri(), FileHelper.PDF_MIME)
        }

        ActivityScenario.launch<PdfReaderActivity>(intent).use { scenario ->
            scenario.onActivity {
                FileOpener.open(it, pdf.toUri(), FileHelper.PDF_MIME, OpenMode.OtherApp)
            }

            val covered = waitUntil { scenario.state != androidx.lifecycle.Lifecycle.State.RESUMED }
            assertTrue("The chooser should cover the reader", covered)
            holdForInspection()
        }
    }

    @Test
    fun readInApp_epubFile_opensTheBuiltInReader() {
        val epub = File(context.cacheDir, "reader-open-test.epub")
        epub.outputStream().use { output ->
            BookDownloader2Helper.writeEpubOfChapters(
                context, "TestApi", "Test Book", "Test Author", null,
                listOf(1 to LoadedChapter("Title 1", "<p>Body of chapter 1</p>")),
                output
            )
        }
        val intent = Intent(context, PdfReaderActivity::class.java).apply {
            setDataAndType(pdf.toUri(), FileHelper.PDF_MIME)
        }

        ActivityScenario.launch<PdfReaderActivity>(intent).use { scenario ->
            scenario.onActivity {
                FileOpener.open(it, epub.toUri(), FileHelper.EPUB_MIME, OpenMode.InApp)
            }

            val opened = waitUntil {
                var found = false
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    found = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED)
                        .any { it is ReadActivity2 }
                }
                found
            }
            holdForInspection()
            epub.delete()
            assertTrue("The built-in EPUB reader should be on screen", opened)
        }
    }

    /** Keeps the screen up for manual screenshots when the run passes `-e holdMs <millis>`. */
    private fun holdForInspection() {
        val millis = InstrumentationRegistry.getArguments().getString("holdMs")?.toLongOrNull() ?: return
        Thread.sleep(millis)
    }

    private var renderedFirstPage = false

    @Test
    fun pdfReader_fileThatCannotBeOpened_closesInsteadOfCrashing() {
        val missing = File(context.cacheDir, "missing.pdf")
        val intent = Intent(context, PdfReaderActivity::class.java).apply {
            setDataAndType(missing.toUri(), FileHelper.PDF_MIME)
        }

        ActivityScenario.launch<PdfReaderActivity>(intent).use { scenario ->
            val closed = waitUntil { scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED }
            assertTrue(closed)
        }
    }

    @Test
    fun exportedFiles_recordsFilesAndForgetsTheDeletedOnes() {
        ExportedFiles.record(novel, "reader-open-test.pdf", pdf.toUri(), FileHelper.PDF_MIME)

        val saved = ExportedFiles.available(context, novel)
        assertEquals(listOf("reader-open-test.pdf"), saved.map { it.name })
        assertTrue(saved.single().isPdf)

        pdf.delete()

        assertTrue(ExportedFiles.available(context, novel).isEmpty())
        assertFalse(ExportedFiles.list(novel).any { it.uri == pdf.toUri().toString() })
    }
}
