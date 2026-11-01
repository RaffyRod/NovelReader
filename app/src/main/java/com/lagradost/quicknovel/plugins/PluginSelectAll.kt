package com.lagradost.quicknovel.plugins

/**
 * Pure helpers for the plugin multi-select dialog (unit-tested).
 */
object PluginSelectAll {
    /** Returns every catalog id — used by “Select all” so Install never sees an empty set. */
    fun allIds(items: List<PluginManifestItem>): LinkedHashSet<String> =
        LinkedHashSet(items.map { it.id })

    fun filterSelected(
        items: List<PluginManifestItem>,
        selectedIds: Set<String>,
    ): List<PluginManifestItem> = items.filter { it.id in selectedIds }
}
