package com.lagradost.quicknovel.plugins

/**
 * Display helpers for the soft-card installed-plugins hub (option B).
 */
object PluginHubUi {
    fun kindLabel(plugin: InstalledPlugin): String =
        when (plugin.templateKind.lowercase()) {
            "madara" -> "Madara"
            "blogger" -> "Blogger"
            else -> if (plugin.cssMapped) "CSS" else "JS"
        }

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

    /** Name / local name / host match for the installed-plugins hub search box. */
    fun matchesQuery(plugin: InstalledPlugin, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim()
        return plugin.localName.contains(q, ignoreCase = true) ||
            plugin.name.contains(q, ignoreCase = true) ||
            siteHost(plugin).contains(q, ignoreCase = true)
    }

    fun filterInstalled(plugins: List<InstalledPlugin>, query: String): List<InstalledPlugin> =
        plugins.filter { matchesQuery(it, query) }
}
