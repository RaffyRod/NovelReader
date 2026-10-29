package com.example.quicknovel

import com.lagradost.quicknovel.ui.common.ImmutableSearchResponse
import com.lagradost.quicknovel.ui.download.LibraryShelves
import com.lagradost.quicknovel.ui.common.staggerDelayMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryShelvesTest {
    private fun novel(name: String, opened: Long?) = ImmutableSearchResponse(
        name = name, apiName = "api", url = name, timeOfCached = 0L,
        timeOfPageOpened = opened, chaptersRead = 0,
    )

    @Test
    fun `the most recently opened novel is the one to continue`() {
        val items = listOf(novel("a", 10), novel("b", 30), novel("c", 20))
        assertEquals("b", LibraryShelves.continueReading(items)?.name)
    }

    @Test
    fun `nothing to continue when no novel was opened`() {
        assertNull(LibraryShelves.continueReading(listOf(novel("a", null), novel("b", 0))))
    }

    @Test
    fun `stagger delay grows with the position and stops growing`() {
        assertEquals(0L, staggerDelayMs(0))
        assertEquals(true, staggerDelayMs(3) > staggerDelayMs(1))
        assertEquals(staggerDelayMs(10), staggerDelayMs(500))
    }
}
