package com.example.quicknovel

import com.lagradost.quicknovel.NewChaptersLogic
import com.lagradost.quicknovel.ui.ReadType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewChaptersLogicTest {
    @Test
    fun `reports the difference when the source has more chapters`() {
        assertEquals(5, NewChaptersLogic.newChapters(known = 100, current = 105))
    }

    @Test
    fun `reports nothing when the chapter count is unchanged`() {
        assertEquals(0, NewChaptersLogic.newChapters(known = 100, current = 100))
    }

    @Test
    fun `reports nothing when the source removed chapters`() {
        assertEquals(0, NewChaptersLogic.newChapters(known = 100, current = 90))
    }

    @Test
    fun `only reading and plan to read novels are followed`() {
        assertTrue(ReadType.READING.prefValue in NewChaptersLogic.followedStates)
        assertTrue(ReadType.PLAN_TO_READ.prefValue in NewChaptersLogic.followedStates)
        assertFalse(ReadType.DROPPED.prefValue in NewChaptersLogic.followedStates)
        assertFalse(ReadType.COMPLETED.prefValue in NewChaptersLogic.followedStates)
        assertFalse(ReadType.NONE.prefValue in NewChaptersLogic.followedStates)
    }
}
