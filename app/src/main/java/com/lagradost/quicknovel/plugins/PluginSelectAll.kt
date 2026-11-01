package com.lagradost.quicknovel.plugins

/**
 * Pure helpers for the plugin multi-select dialog (unit-tested).
 */
object PluginSelectAll {
    /** Returns every catalog id — used by “Select all” so Install never sees an empty set. */
    fun allIds(items: List<PluginManifestItem>): LinkedHashSet<String> =
        LinkedHashSet(items.map { it.id })

    /**
     * Ids that can be newly installed. Already-installed plugins are excluded so a bulk add
     * never rewrites existing JS / CSS / local names.
     */
    fun installableIds(
        items: List<PluginManifestItem>,
        installedIds: Set<String>,
    ): LinkedHashSet<String> =
        LinkedHashSet(items.map { it.id }.filter { it !in installedIds })

    /** True when the catalog version differs from what is installed (update available). */
    fun hasCatalogUpdate(installed: InstalledPlugin, catalog: PluginManifestItem): Boolean {
        val remote = catalog.version.trim()
        return remote.isNotBlank() && remote != installed.version.trim()
    }

    /**
     * Selectable = not installed yet, or installed with a newer catalog version.
     * Same-version installs stay locked.
     */
    fun isSelectable(
        item: PluginManifestItem,
        installedById: Map<String, InstalledPlugin>,
    ): Boolean {
        val installed = installedById[item.id] ?: return true
        return hasCatalogUpdate(installed, item)
    }

    fun selectableIds(
        items: List<PluginManifestItem>,
        installedById: Map<String, InstalledPlugin>,
    ): LinkedHashSet<String> =
        LinkedHashSet(items.filter { isSelectable(it, installedById) }.map { it.id })

    fun filterSelected(
        items: List<PluginManifestItem>,
        selectedIds: Set<String>,
    ): List<PluginManifestItem> = items.filter { it.id in selectedIds }

    /** Keeps only not-yet-installed items from a selection. */
    fun filterNewInstalls(
        items: List<PluginManifestItem>,
        selectedIds: Set<String>,
        installedIds: Set<String>,
    ): List<PluginManifestItem> =
        filterSelected(items, selectedIds).filter { it.id !in installedIds }

    /** Keeps new installs + updatable installed plugins from a selection. */
    fun filterSelectable(
        items: List<PluginManifestItem>,
        selectedIds: Set<String>,
        installedById: Map<String, InstalledPlugin>,
    ): List<PluginManifestItem> =
        filterSelected(items, selectedIds).filter { isSelectable(it, installedById) }
}
