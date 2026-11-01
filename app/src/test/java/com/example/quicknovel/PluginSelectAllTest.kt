package com.example.quicknovel

import com.lagradost.quicknovel.plugins.PluginManifestItem
import com.lagradost.quicknovel.plugins.PluginSelectAll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginSelectAllTest {
    private val catalog = listOf(
        PluginManifestItem(id = "a", name = "A"),
        PluginManifestItem(id = "b", name = "B"),
        PluginManifestItem(id = "c", name = "C"),
    )

    @Test
    fun allIds_includesEveryCatalogEntry() {
        val ids = PluginSelectAll.allIds(catalog)
        assertEquals(setOf("a", "b", "c"), ids)
        assertEquals(3, ids.size)
    }

    @Test
    fun filterSelected_keepsOnlyCheckedIds() {
        val chosen = PluginSelectAll.filterSelected(catalog, setOf("b", "c"))
        assertEquals(listOf("b", "c"), chosen.map { it.id })
    }

    @Test
    fun selectAllThenFilter_returnsFullCatalog() {
        val selected = PluginSelectAll.allIds(catalog)
        val chosen = PluginSelectAll.filterSelected(catalog, selected)
        assertEquals(3, chosen.size)
        assertTrue(chosen.map { it.id }.containsAll(listOf("a", "b", "c")))
    }
}
