package com.lagradost.quicknovel.providers

import java.net.URI

/** Ready made site layouts, so a provider can be added with only a name and a web address. */
object CustomProviderPresets {
    /** Searches tried in order, some sites return nothing for very short or foreign words. */
    val testQueries = listOf("the", "a", "el")

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

    /** Layouts to try for [baseUrl], most common first. */
    fun candidates(name: String, baseUrl: String, lang: String): List<CustomProviderConfig> {
        val madara = CustomProviderConfig.madaraTemplate(name, baseUrl).copy(lang = lang)
        return listOf(
            madara,
            madara.copy(chaptersViaAjax = false),
        )
    }
}
