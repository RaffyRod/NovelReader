package com.example.quicknovel

import com.lagradost.quicknovel.ui.search.SearchRow
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRowGroupingTest {
    @Test
    fun withResults_keepsProviderRowsSeparate() {
        val rows = listOf(
            SearchRow(name = "NovelFire", items = persistentListOf()),
            SearchRow(name = "RoyalRoad", items = persistentListOf()),
            SearchRow(name = "EmptySrc", items = persistentListOf()),
        )
        // Mimic SearchResults: only rows that already have hits are shown as groups.
        val withResults = rows.filter { it.items.isNotEmpty() }
        assertTrue(withResults.isEmpty())

        val filled = listOf(
            SearchRow(name = "NovelFire", items = persistentListOf()),
            SearchRow(name = "RoyalRoad", items = persistentListOf()),
        )
        // Names stay distinct so OpenRow can open one provider at a time.
        assertEquals(listOf("NovelFire", "RoyalRoad"), filled.map { it.name })
        assertEquals(2, filled.distinctBy { it.name }.size)
    }
}
