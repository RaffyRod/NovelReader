package com.lagradost.quicknovel.plugins

/**
 * Pure catalog list helpers: text + status + kind filters and paging (unit-tested).
 * There is no hard cap of 100 — UI pages a filtered list (default [DEFAULT_PAGE_SIZE]).
 */
object PluginCatalogFilter {
    const val DEFAULT_PAGE_SIZE = 60

    enum class Status {
        ALL,
        NEW,
        UPDATES,
        INSTALLED,
    }

    enum class Kind {
        ALL,
        MADARA,
        BLOGGER,
        JS,
    }

    fun matchesQuery(item: PluginManifestItem, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim()
        return item.name.contains(q, ignoreCase = true) ||
            item.site.contains(q, ignoreCase = true) ||
            item.id.contains(q, ignoreCase = true) ||
            item.lang.contains(q, ignoreCase = true) ||
            item.displayLang.contains(q, ignoreCase = true) ||
            PluginHubUi.siteHost(item.site).contains(q, ignoreCase = true)
    }

    fun matchesStatus(
        item: PluginManifestItem,
        status: Status,
        installedById: Map<String, InstalledPlugin>,
    ): Boolean {
        val installed = installedById[item.id]
        return when (status) {
            Status.ALL -> true
            Status.NEW -> installed == null
            Status.UPDATES -> installed != null && PluginSelectAll.hasCatalogUpdate(installed, item)
            Status.INSTALLED -> installed != null
        }
    }

    fun matchesKind(item: PluginManifestItem, kind: Kind): Boolean {
        if (kind == Kind.ALL) return true
        return when (PluginTemplateHints.detect(item)) {
            PluginTemplateKind.MADARA -> kind == Kind.MADARA
            PluginTemplateKind.BLOGGER -> kind == Kind.BLOGGER
            PluginTemplateKind.UNSUPPORTED_JS -> kind == Kind.JS
        }
    }

    fun filter(
        items: List<PluginManifestItem>,
        query: String = "",
        status: Status = Status.ALL,
        kind: Kind = Kind.ALL,
        installedById: Map<String, InstalledPlugin> = emptyMap(),
    ): List<PluginManifestItem> = items.filter {
        matchesQuery(it, query) &&
            matchesStatus(it, status, installedById) &&
            matchesKind(it, kind)
    }

    fun pageCount(total: Int, pageSize: Int = DEFAULT_PAGE_SIZE): Int =
        if (total <= 0) 1 else ((total + pageSize - 1) / pageSize)

    fun pageSlice(
        items: List<PluginManifestItem>,
        page: Int,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): List<PluginManifestItem> {
        val count = pageCount(items.size, pageSize)
        val safe = page.coerceIn(0, count - 1)
        val from = safe * pageSize
        return items.drop(from).take(pageSize)
    }
}
