package com.lagradost.quicknovel

import com.lagradost.quicknovel.ui.ReaderGlyphLoading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderGlyphLoadingTest {
    private val loadingChapter = "Cargando capítulo…"
    private val loadingFallback = "Cargando…"

    @Test
    fun blankStatus_usesChapterTitleAndSourceChip() {
        val ui = ReaderGlyphLoading.resolve(
            status = null,
            chapterTitle = "12. Storm",
            sourceChip = "NovelFire",
            loadingChapterLabel = loadingChapter,
            loadingFallback = loadingFallback,
        )
        assertEquals("12. Storm", ui.title)
        assertEquals(loadingChapter, ui.subtitle)
        assertEquals("NovelFire", ui.sourceChip)
    }

    @Test
    fun blankStatus_withoutChapter_usesFallback() {
        val ui = ReaderGlyphLoading.resolve(
            status = "   ",
            chapterTitle = null,
            sourceChip = null,
            loadingChapterLabel = loadingChapter,
            loadingFallback = loadingFallback,
        )
        assertEquals(loadingFallback, ui.title)
        assertNull(ui.sourceChip)
    }

    @Test
    fun httpUrl_neverShownAsTitle_hostGoesToChip() {
        val ui = ReaderGlyphLoading.resolve(
            status = "https://www.novelfire.net/book/x/chapter-12",
            chapterTitle = "12. Storm",
            sourceChip = "NovelFire",
            loadingChapterLabel = loadingChapter,
            loadingFallback = loadingFallback,
        )
        assertEquals("12. Storm", ui.title)
        assertEquals(loadingChapter, ui.subtitle)
        assertEquals("novelfire.net", ui.sourceChip)
        assertFalse(ui.title.startsWith("http"))
    }

    @Test
    fun chapterTitleStatus_showsAsTitle() {
        val ui = ReaderGlyphLoading.resolve(
            status = "12. Storm",
            chapterTitle = "12. Storm",
            sourceChip = "QuickNovel Ultimate",
            loadingChapterLabel = loadingChapter,
            loadingFallback = loadingFallback,
        )
        assertEquals("12. Storm", ui.title)
        assertEquals(loadingChapter, ui.subtitle)
    }

    @Test
    fun translatingProgress_goesToSubtitle() {
        val progress = "Traduciendo 12. Storm (3/40)"
        val ui = ReaderGlyphLoading.resolve(
            status = progress,
            chapterTitle = "12. Storm",
            sourceChip = "NovelFire",
            loadingChapterLabel = loadingChapter,
            loadingFallback = loadingFallback,
        )
        assertEquals("12. Storm", ui.title)
        assertEquals(progress, ui.subtitle)
    }

    @Test
    fun hostFromUrl_stripsWwwAndPath() {
        assertEquals(
            "example.com",
            ReaderGlyphLoading.hostFromUrl("https://www.example.com/a/b?q=1"),
        )
        assertEquals(
            "cdn.site.io",
            ReaderGlyphLoading.hostFromUrl("http://cdn.site.io:8080/x"),
        )
        assertNull(ReaderGlyphLoading.hostFromUrl("not-a-url"))
    }

    @Test
    fun isHttpUrl_detectsSchemes() {
        assertTrue(ReaderGlyphLoading.isHttpUrl("HTTPS://x.com"))
        assertFalse(ReaderGlyphLoading.isHttpUrl("12. Storm"))
    }
}
