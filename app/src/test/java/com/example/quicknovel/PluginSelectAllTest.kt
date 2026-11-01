package com.example.quicknovel

import com.lagradost.quicknovel.plugins.InstalledPlugin
import com.lagradost.quicknovel.plugins.PluginManifestItem
import com.lagradost.quicknovel.plugins.PluginSelectAll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginSelectAllTest {
    private val catalog = listOf(
        PluginManifestItem(id = "a", name = "A", version = "1.0.0"),
        PluginManifestItem(id = "b", name = "B", version = "2.0.0"),
        PluginManifestItem(id = "c", name = "C", version = "1.1.0"),
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

    @Test
    fun installableIds_excludesAlreadyInstalled() {
        val ids = PluginSelectAll.installableIds(catalog, setOf("b"))
        assertEquals(setOf("a", "c"), ids)
    }

    @Test
    fun filterNewInstalls_skipsInstalledEvenIfChecked() {
        val chosen = PluginSelectAll.filterNewInstalls(
            catalog,
            selectedIds = setOf("a", "b"),
            installedIds = setOf("b"),
        )
        assertEquals(listOf("a"), chosen.map { it.id })
    }

    @Test
    fun selectableIds_allowsNewAndUpdatesOnly() {
        val installed = mapOf(
            "b" to InstalledPlugin(id = "b", name = "B", version = "2.0.0"),
            "c" to InstalledPlugin(id = "c", name = "C", version = "1.0.0"),
        )
        val ids = PluginSelectAll.selectableIds(catalog, installed)
        assertEquals(setOf("a", "c"), ids)
        assertFalse(PluginSelectAll.isSelectable(catalog[1], installed))
        assertTrue(PluginSelectAll.hasCatalogUpdate(installed.getValue("c"), catalog[2]))
    }

    @Test
    fun filterSelectable_keepsNewAndUpdatesFromSelection() {
        val installed = mapOf(
            "a" to InstalledPlugin(id = "a", name = "A", version = "1.0.0"),
            "c" to InstalledPlugin(id = "c", name = "C", version = "1.0.0"),
        )
        val chosen = PluginSelectAll.filterSelectable(
            catalog,
            selectedIds = setOf("a", "b", "c"),
            installedById = installed,
        )
        // a same version → locked out; b new; c update
        assertEquals(listOf("b", "c"), chosen.map { it.id })
    }
}
