package com.lagradost.quicknovel.plugins

/**
 * Display helpers for the soft-card installed-plugins hub (smart default / mock M).
 */
object PluginHubUi {
    fun kindLabel(plugin: InstalledPlugin): String =
        when (plugin.templateKind.lowercase()) {
            "madara" -> "Madara"
            "blogger" -> "Blogger"
            else -> if (plugin.cssMapped) "CSS" else "JS"
        }

    /** Count of installed plugins already mapped into Search. */
    fun inSearchCount(plugins: List<InstalledPlugin>): Int = plugins.count { it.inSearch }

    fun pendingSearchCount(plugins: List<InstalledPlugin>): Int =
        plugins.size - inSearchCount(plugins)

    fun kindLabel(item: PluginManifestItem): String =
        when (PluginTemplateHints.detect(item)) {
            PluginTemplateKind.MADARA -> "Madara"
            PluginTemplateKind.BLOGGER -> "Blogger"
            PluginTemplateKind.UNSUPPORTED_JS -> "JS"
        }

    /**
     * @param notInSearchLabel localized label (e.g. ES "sin búsqueda", EN "not in Search")
     */
    fun metaLine(
        plugin: InstalledPlugin,
        notInSearchLabel: String = "not in Search",
    ): String = buildString {
        append("v")
        append(plugin.version.ifBlank { "—" })
        append(" · ")
        append(plugin.lang.ifBlank { plugin.langCode }.ifBlank { "—" })
        append(" · ")
        append(kindLabel(plugin))
        if (!plugin.inSearch) {
            append(" · ")
            append(notInSearchLabel)
        }
        if (plugin.hasUpdate) {
            append(" · ↑ ")
            append(plugin.latestVersion)
        }
    }

    fun catalogMetaLine(item: PluginManifestItem): String = buildString {
        append("v")
        append(item.version.ifBlank { "—" })
        append(" · ")
        append(item.displayLang.ifBlank { "—" })
        append(" · ")
        append(kindLabel(item))
    }

    fun siteHost(plugin: InstalledPlugin): String = siteHost(plugin.site)

    fun siteHost(site: String): String {
        val raw = site.trim()
        if (raw.isBlank()) return ""
        return raw
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
    }

    /**
     * Host / URL match: exact host, subdomain, or full URL contains when [query] has a scheme.
     * Token-aware so `a.test` does not match `beta.test`.
     */
    fun siteMatches(siteOrHost: String, query: String): Boolean {
        if (siteOrHost.isBlank() || query.isBlank()) return false
        val q = query.trim()
        if ("://" in q && siteOrHost.contains(q, ignoreCase = true)) return true
        val host = siteHost(siteOrHost).lowercase().removePrefix("www.")
        val needle = siteHost(q).ifBlank { q }
            .lowercase()
            .removePrefix("www.")
            .trimEnd('/')
        if (host.isBlank() || needle.isBlank()) return false
        return host == needle ||
            host.endsWith(".$needle") ||
            host.startsWith("$needle.")
    }

    /** Name / local name / host match for the installed-plugins hub search box. */
    fun matchesQuery(plugin: InstalledPlugin, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim()
        return plugin.localName.contains(q, ignoreCase = true) ||
            plugin.name.contains(q, ignoreCase = true) ||
            siteMatches(plugin.site, q) ||
            siteMatches(siteHost(plugin), q)
    }

    fun filterInstalled(plugins: List<InstalledPlugin>, query: String): List<InstalledPlugin> =
        plugins.filter { matchesQuery(it, query) }
}
