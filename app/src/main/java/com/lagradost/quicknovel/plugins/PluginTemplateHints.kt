package com.lagradost.quicknovel.plugins

/**
 * Detects which CSS template (if any) matches an LNReader catalog entry.
 * Madara defaults must NOT be applied to every plugin — only Madara / close family.
 */
enum class PluginTemplateKind {
    MADARA,
    BLOGGER,
    /** Site known but no CSS template in Ultimate — do not invent Madara selectors. */
    UNSUPPORTED_JS,
}

object PluginTemplateHints {
    private val bracketTag = Regex("""\[([^\]]+)\]""")
    private val multisrc = Regex("""multisrc/([^/]+)/""")

    /**
     * Only catalog metadata (url / icon / id / name / site) is used.
     * Never scan the JS body: shared LNReader helpers mention Madara and would force the same
     * `/?s={query}&post_type=wp-manga` search path onto every plugin.
     */
    fun detect(item: PluginManifestItem, jsSource: String = ""): PluginTemplateKind {
        // jsSource intentionally unused for detection (kept for call-site compatibility).
        @Suppress("UNUSED_PARAMETER")
        val ignoredJs = jsSource

        val meta = listOf(item.id, item.url, item.iconUrl, item.name, item.site)
            .joinToString("\n")
            .lowercase()

        if ("blogspot" in meta || item.site.contains("blogger", ignoreCase = true)) {
            return PluginTemplateKind.BLOGGER
        }

        val tag = bracketTag.find(item.url)?.groupValues?.getOrNull(1)?.lowercase()
            ?: multisrc.find(item.iconUrl)?.groupValues?.getOrNull(1)?.lowercase()
            ?: bracketTag.find(item.id)?.groupValues?.getOrNull(1)?.lowercase()
            ?: multisrc.find(item.url)?.groupValues?.getOrNull(1)?.lowercase()

        return when (tag) {
            "madara", "lightnovelwp" -> PluginTemplateKind.MADARA
            "blogger" -> PluginTemplateKind.BLOGGER
            else -> PluginTemplateKind.UNSUPPORTED_JS
        }
    }
}
