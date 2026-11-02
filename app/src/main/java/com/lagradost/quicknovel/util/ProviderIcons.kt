package com.lagradost.quicknovel.util

import java.net.URI

/** Icon resolution for Search provider tiles (built-in drawable, remote URL, or initials). */
object ProviderIcons {
    /**
     * One–two letter initials for the placeholder tile when no drawable / URL is available.
     */
    fun initials(name: String): String {
        val parts = name.trim()
            .split(Regex("[\\s\\-_./]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> buildString {
                append(parts[0].first().uppercaseChar())
                append(parts[1].first().uppercaseChar())
            }
        }
    }

    /** Google favicon proxy for [baseUrl]'s host, or null when the URL has no host. */
    fun faviconUrl(baseUrl: String): String? {
        val host = runCatching { URI(baseUrl.trim()).host }
            .getOrNull()
            ?.trim()
            ?.trimStart('.')
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return "https://www.google.com/s2/favicons?domain=$host&sz=128"
    }

    /**
     * Prefer an explicit [iconUrl]; otherwise a favicon derived from [mainUrl].
     * Blank / invalid → null (UI shows initials placeholder).
     */
    fun resolvedIconUrl(iconUrl: String?, mainUrl: String): String? {
        val explicit = iconUrl?.trim()?.takeIf { it.isNotBlank() }
        if (explicit != null) return explicit
        return faviconUrl(mainUrl)
    }
}
