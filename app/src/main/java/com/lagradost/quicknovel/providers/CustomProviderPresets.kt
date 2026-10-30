package com.lagradost.quicknovel.providers

import java.net.URI

/** Ready made site layouts, so a provider can be added with only a name and a web address. */
object CustomProviderPresets {
    /** Searches tried in order, some sites return nothing for very short or foreign words. */
    val testQueries = listOf("the", "a", "el")

    /**
     * Blogger catalogs often use Spanish titles, so English words like "the" find nothing.
     * An empty query lists every novel when the blog has an index page.
     */
    val bloggerTestQueries = listOf("", "a", "el", "la", "the")

    /** Turns what a person types ("misitio.com/novelas") into the site root ("https://misitio.com"). */
    fun normalizeBaseUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank() || trimmed.contains(' ')) return null
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
        val host = uri.host?.takeIf { it.contains('.') } ?: return null
        val port = if (uri.port != -1) ":${uri.port}" else ""
        return "${uri.scheme}://$host$port"
    }

    /**
     * Layouts to try for [baseUrl].
     * Pass [kind] when the user already chose Blog or Website so only that layout is tested.
     */
    fun candidates(
        name: String,
        baseUrl: String,
        lang: String,
        kind: String? = null,
    ): List<CustomProviderConfig> {
        val madara = CustomProviderConfig.madaraTemplate(name, baseUrl).copy(lang = lang)
        val blogger = CustomProviderConfig.bloggerTemplate(name, baseUrl).copy(lang = lang)
        val sites = listOf(madara, madara.copy(chaptersViaAjax = false))
        return when (kind) {
            CustomProviderConfig.KIND_BLOGGER -> listOf(blogger)
            CustomProviderConfig.KIND_CSS -> sites
            else -> if (baseUrl.contains("blogspot.")) listOf(blogger) + sites else sites + blogger
        }
    }
}
