package com.lagradost.quicknovel.util

import android.content.Context
import androidx.preference.PreferenceManager
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.plugins.InstalledPlugin
import com.lagradost.quicknovel.plugins.PluginHubUi
import com.lagradost.quicknovel.plugins.PluginStore
import com.lagradost.quicknovel.providers.CustomProviderConfig

/**
 * Unified **Fuentes de Buscar** (hub mock N): built-in APIs + custom CSS + LNReader plugins.
 */
object ProviderManage {
    const val PAGE_SIZE = 60

    enum class Kind { BUILTIN, CUSTOM, PLUGIN }

    /** Activas = toggled in Search · Instaladas = customs + plugins · Todas = everything. */
    enum class Tab { ACTIVE, INSTALLED, ALL }

    data class Row(
        val key: String,
        val title: String,
        val subtitle: String,
        val kind: Kind,
        /** Name stored in [R.string.search_providers_list_key]. Empty when plugin is not CSS-mapped. */
        val searchName: String,
        val activeInSearch: Boolean,
        /** False when a plugin still needs «Elegir plantilla» before it can join Search. */
        val canToggleSearch: Boolean,
        val config: CustomProviderConfig? = null,
        val plugin: InstalledPlugin? = null,
    )

    fun readActiveNames(context: Context): Set<String> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = context.getString(R.string.search_providers_list_key)
        val saved = prefs.getStringSet(key, null)
        return if (saved == null) {
            Apis.apis.map { it.name }.toSet()
        } else {
            HashSet(saved)
        }
    }

    /**
     * Built-ins + customs + plugins (plugins already represented by a custom name are skipped).
     */
    fun loadRows(activeNames: Set<String>): List<Row> {
        val customs = CustomProviders.load()
        val customNames = customs.map { it.name.trim().lowercase() }.toSet()

        val builtinRows = Apis.apis.map { api ->
            Row(
                key = "builtin:${api.name}",
                title = api.name,
                subtitle = "Built-in · ${api.lang}",
                kind = Kind.BUILTIN,
                searchName = api.name,
                activeInSearch = activeNames.contains(api.name),
                canToggleSearch = true,
            )
        }

        val customRows = customs.map { cfg ->
            val host = PluginHubUi.siteHost(cfg.baseUrl).ifBlank { cfg.kind }
            Row(
                key = "custom:${cfg.name}",
                title = cfg.name,
                subtitle = "$host · Proveedor",
                kind = Kind.CUSTOM,
                searchName = cfg.name,
                activeInSearch = activeNames.contains(cfg.name),
                canToggleSearch = true,
                config = cfg,
            )
        }

        val pluginRows = PluginStore.loadInstalled()
            .filter { plugin ->
                val mapped = plugin.providerName.trim()
                mapped.isBlank() || mapped.lowercase() !in customNames
            }
            .map { plugin ->
                val mapped = plugin.providerName.trim()
                val inSearch = plugin.inSearch && mapped.isNotBlank() && activeNames.contains(mapped)
                val host = PluginHubUi.siteHost(plugin)
                Row(
                    key = "plugin:${plugin.id}",
                    title = plugin.localName.ifBlank { plugin.name },
                    // Clean line: host · Plugin (no JS / sin búsqueda clutter — mock A)
                    subtitle = if (host.isNotBlank()) "$host · Plugin" else "Plugin",
                    kind = Kind.PLUGIN,
                    searchName = mapped,
                    activeInSearch = inSearch,
                    canToggleSearch = plugin.inSearch && mapped.isNotBlank(),
                    plugin = plugin,
                )
            }

        return (builtinRows + customRows + pluginRows).sortedBy { it.title.lowercase() }
    }

    /** Manageable count for Settings subtitle (customs + plugins, not built-ins). */
    fun count(): Int = loadRows(emptySet()).count { it.kind != Kind.BUILTIN }

    fun filterTab(rows: List<Row>, tab: Tab): List<Row> =
        when (tab) {
            Tab.ALL -> rows
            Tab.ACTIVE -> rows.filter { it.activeInSearch }
            Tab.INSTALLED -> rows.filter { it.kind == Kind.CUSTOM || it.kind == Kind.PLUGIN }
        }

    /**
     * Match by display name, Search name, or site/host (page URL).
     * Host matching is token-aware so `a.test` does not hit `beta.test`.
     */
    fun matchesName(row: Row, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim()
        if (row.title.contains(q, ignoreCase = true)) return true
        if (row.searchName.contains(q, ignoreCase = true)) return true
        val site = row.config?.baseUrl.orEmpty().ifBlank { row.plugin?.site.orEmpty() }
        if (PluginHubUi.siteMatches(site, q)) return true
        val subtitleHost = row.subtitle.substringBefore('·').trim()
        if (PluginHubUi.siteMatches(subtitleHost, q)) return true
        // Plain-text subtitle (e.g. "Built-in · es") — skip dotted host queries.
        return !q.contains('.') && row.subtitle.contains(q, ignoreCase = true)
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
