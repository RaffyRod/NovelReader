package com.example.quicknovel

import com.lagradost.quicknovel.ui.result.SelectionTaskKind
import com.lagradost.quicknovel.ui.result.SelectionTaskState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionTaskStateTest {

    @Test
    fun unfinishedTranslateState_isNotFinished() {
        val state = SelectionTaskState(
            kind = SelectionTaskKind.Download,
            done = 2,
            total = 10,
            translating = true,
        )
        assertFalse(state.finished)
        assertTrue(state.translating)
        assertEquals(2, state.done)
    }

    @Test
    fun finishedState_clearsActiveWorkFlagSemantics() {
        val state = SelectionTaskState(
            kind = SelectionTaskKind.Download,
            done = 10,
            total = 10,
            finished = true,
        )
        assertTrue(state.finished)
        assertFalse(state.translating)
    }
}
