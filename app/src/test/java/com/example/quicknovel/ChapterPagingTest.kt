package com.example.quicknovel

import com.lagradost.quicknovel.ChapterPaging
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterPagingTest {
    private val chapters = (1..95).toList()

    @Test
    fun `chapter 500 of a 5000 chapter novel lands on the right page for each size`() {
        assertEquals(49, ChapterPaging.pageOfPosition(500, 5000, 10))
        assertEquals(24, ChapterPaging.pageOfPosition(500, 5000, 20))
        assertEquals(16, ChapterPaging.pageOfPosition(500, 5000, 30))
        assertEquals(499, ChapterPaging.pageOfPosition(5000, 5000, 10))
        assertEquals(0, ChapterPaging.pageOfPosition(500, 5000, ChapterPaging.ALL))
    }

    @Test
    fun `chapters outside the list have no page`() {
        assertNull(ChapterPaging.pageOfPosition(0, 5000, 10))
        assertNull(ChapterPaging.pageOfPosition(5001, 5000, 10))
    }

    @Test
    fun `page 500 exists with 10 per page and is the last one for 5000 chapters`() {
        val page = ChapterPaging.pageOf((1..5000).toList(), 499, 10)
        assertEquals(500, page.pageCount)
        assertEquals(4991, page.firstPosition)
        assertEquals(5000, page.lastPosition)
    }

    @Test
    fun `presets are 10, 20 and 30 chapters per page`() {
        assertEquals(listOf(10, 20, 30), ChapterPaging.PRESETS)
    }

    @Test
    fun `page count rounds up and an empty list still has one page`() {
        assertEquals(10, ChapterPaging.pageCount(95, 10))
        assertEquals(5, ChapterPaging.pageCount(95, 20))
        assertEquals(4, ChapterPaging.pageCount(95, 30))
        assertEquals(3, ChapterPaging.pageCount(90, 30))
        assertEquals(1, ChapterPaging.pageCount(0, 20))
        assertEquals(1, ChapterPaging.pageCount(95, ChapterPaging.ALL))
    }

    @Test
    fun `each page holds its own chapters in order`() {
        val first = ChapterPaging.pageOf(chapters, 0, 20)
        val third = ChapterPaging.pageOf(chapters, 2, 20)
        val last = ChapterPaging.pageOf(chapters, 4, 20)

        assertEquals((1..20).toList(), first.items)
        assertEquals((41..60).toList(), third.items)
        assertEquals((81..95).toList(), last.items)
        assertTrue(first.hasNext)
        assertFalse(first.hasPrevious)
        assertFalse(last.hasNext)
        assertTrue(last.hasPrevious)
    }

    @Test
    fun `positions shown to the reader are one based and cover the page`() {
        val page = ChapterPaging.pageOf(chapters, 4, 20)

        assertEquals(81, page.firstPosition)
        assertEquals(95, page.lastPosition)
        assertEquals(95, page.total)
    }

    @Test
    fun `a page out of range is moved to the nearest valid one`() {
        assertEquals(0, ChapterPaging.pageOf(chapters, -3, 30).page)
        assertEquals(3, ChapterPaging.pageOf(chapters, 99, 30).page)
        assertEquals((91..95).toList(), ChapterPaging.pageOf(chapters, 99, 10).items.takeLast(5))
    }

    @Test
    fun `all chapters in one page when the size is all`() {
        val page = ChapterPaging.pageOf(chapters, 5, ChapterPaging.ALL)

        assertEquals(chapters, page.items)
        assertEquals(0, page.page)
        assertEquals(1, page.pageCount)
        assertFalse(page.hasNext)
    }

    @Test
    fun `empty list gives an empty page without positions`() {
        val page = ChapterPaging.pageOf(emptyList<Int>(), 0, 20)

        assertTrue(page.items.isEmpty())
        assertEquals(0, page.firstPosition)
        assertEquals(0, page.lastPosition)
    }

    @Test
    fun `changing the size keeps the chapter the reader was looking at`() {
        val onPageThree = ChapterPaging.pageOf(chapters, 2, 10)

        assertEquals(1, ChapterPaging.pageAfterResize(onPageThree, 20))
        assertEquals(0, ChapterPaging.pageAfterResize(onPageThree, 30))
        assertEquals(0, ChapterPaging.pageAfterResize(onPageThree, ChapterPaging.ALL))
        assertEquals(4, ChapterPaging.pageAfterResize(onPageThree, 5))
    }

    @Test
    fun `custom sizes must be between 1 and the maximum`() {
        assertEquals(15, ChapterPaging.parseSize(" 15 "))
        assertEquals(200, ChapterPaging.parseSize("200"))
        assertNull(ChapterPaging.parseSize("0"))
        assertNull(ChapterPaging.parseSize("201"))
        assertNull(ChapterPaging.parseSize("-4"))
        assertNull(ChapterPaging.parseSize("abc"))
        assertNull(ChapterPaging.parseSize(""))
    }

    @Test
    fun `sizes are normalized`() {
        assertEquals(ChapterPaging.ALL, ChapterPaging.normalizeSize(0))
        assertEquals(ChapterPaging.ALL, ChapterPaging.normalizeSize(-5))
        assertEquals(ChapterPaging.MAX_SIZE, ChapterPaging.normalizeSize(5000))
        assertEquals(30, ChapterPaging.normalizeSize(30))
    }
}
