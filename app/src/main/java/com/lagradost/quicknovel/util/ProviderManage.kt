package com.lagradost.quicknovel.util

import com.lagradost.quicknovel.plugins.InstalledPlugin
import com.lagradost.quicknovel.plugins.PluginHubUi
import com.lagradost.quicknovel.plugins.PluginStore
import com.lagradost.quicknovel.providers.CustomProviderConfig

/**
 * Unified rows for **Administrar proveedores**: URL/JSON customs + installed LNReader
 * plugins (catalog install no longer auto-creates CSS, so plugins must appear here).
 */
object ProviderManage {
    const val PAGE_SIZE = 60

    enum class Kind { CUSTOM, PLUGIN }

    data class Row(
        val key: String,
        val title: String,
        val subtitle: String,
        val kind: Kind,
        val config: CustomProviderConfig? = null,
        val plugin: InstalledPlugin? = null,
    )

    /** Deduped manageables — customs first, then plugins not already represented by a custom. */
    fun loadRows(): List<Row> {
        val customs = CustomProviders.load()
        val customNames = customs.map { it.name.trim().lowercase() }.toSet()
        val customRows = customs.map { cfg ->
            Row(
                key = "custom:${cfg.name}",
                title = cfg.name,
                subtitle = cfg.baseUrl.ifBlank { cfg.kind },
                kind = Kind.CUSTOM,
                config = cfg,
            )
        }
        val pluginRows = PluginStore.loadInstalled()
            .filter { plugin ->
                val mapped = plugin.providerName.trim()
                mapped.isBlank() || mapped.lowercase() !in customNames
            }
            .map { plugin ->
                Row(
                    key = "plugin:${plugin.id}",
                    title = plugin.localName.ifBlank { plugin.name },
                    subtitle = buildString {
                        val host = PluginHubUi.siteHost(plugin)
                        if (host.isNotBlank()) append(host).append(" · ")
                        append(PluginHubUi.kindLabel(plugin))
                        if (!plugin.inSearch) append(" · sin búsqueda")
                    },
                    kind = Kind.PLUGIN,
                    plugin = plugin,
                )
            }
        return (customRows + pluginRows).sortedBy { it.title.lowercase() }
    }

    fun count(): Int = loadRows().size

    fun matchesName(row: Row, query: String): Boolean {
        if (query.isBlank()) return true
        return row.title.contains(query.trim(), ignoreCase = true)
    }

    fun filter(rows: List<Row>, query: String): List<Row> =
        rows.filter { matchesName(it, query) }

    fun pageCount(total: Int, pageSize: Int = PAGE_SIZE): Int =
        if (total <= 0) 1 else ((total + pageSize - 1) / pageSize)

    fun pageSlice(rows: List<Row>, page: Int, pageSize: Int = PAGE_SIZE): List<Row> {
        val count = pageCount(rows.size, pageSize)
        val safe = page.coerceIn(0, count - 1)
        val from = safe * pageSize
        return rows.drop(from).take(pageSize)
    }

    fun parseGoToPage(raw: String?, pageCount: Int): Int? {
        if (pageCount <= 0) return null
        val n = raw?.trim()?.toIntOrNull() ?: return null
        return n.coerceIn(1, pageCount) - 1
    }
}
