package com.lagradost.quicknovel.ui.search

import com.lagradost.quicknovel.MainAPI
import com.lagradost.quicknovel.plugins.PluginHubUi

/**
 * When the user types a provider / site identity in Buscar (e.g. "Wattpad", "wattpad.com"),
 * scope the novel search to that provider so other Fuentes do not flood the results.
 */
object SearchProviderScope {
    data class Resolution(
        /** Providers to query (may be a single site when [query] names it). */
        val apis: List<MainAPI>,
        /**
         * Novel text sent to [MainAPI.search]. Empty when the whole query was only the
         * provider identity (open that site’s search/home via empty query).
         */
        val novelQuery: String,
        /** True when [query] uniquely identified one provider among [candidates]. */
        val scopedToProvider: Boolean,
    )

    /**
     * Picks providers among [candidates] (already Fuentes-enabled).
     * - Unique name/host match → only that API; strip the identity from the novel query.
     * - Otherwise → all [candidates] with the original [query].
     */
    fun resolve(query: String, candidates: List<MainAPI>): Resolution {
        val raw = query.trim()
        if (raw.isEmpty() || candidates.isEmpty()) {
            return Resolution(candidates, raw, scopedToProvider = false)
        }
        val matches = candidates.filter { matchesIdentity(it, raw) }
        if (matches.size == 1) {
            val api = matches.first()
            // Whole query was the site identity (incl. typos like "wattap") → no novel text.
            val novelQuery = if (' ' in raw) stripIdentity(raw, api).trim() else ""
            return Resolution(listOf(api), novelQuery, scopedToProvider = true)
        }
        // "wattpad romance" — name as first token
        val first = raw.substringBefore(' ').trim()
        val rest = raw.substringAfter(' ', "").trim()
        if (first.length >= 3 && rest.isNotEmpty()) {
            val byToken = candidates.filter { matchesIdentity(it, first) }
            if (byToken.size == 1) {
                return Resolution(byToken, rest, scopedToProvider = true)
            }
        }
        return Resolution(candidates, raw, scopedToProvider = false)
    }

    /**
     * Live Buscar grid filter: match provider display name or site/host (page name).
     * Looser than [matchesIdentity] so short prefixes still shrink the tile list.
     */
    fun matchesGridFilter(api: MainAPI, query: String): Boolean {
        val q = query.trim()
        if (q.isBlank()) return true
        if (api.name.contains(q, ignoreCase = true)) return true
        if (PluginHubUi.siteMatches(api.mainUrl, q)) return true
        val host = PluginHubUi.siteHost(api.mainUrl)
        if (host.contains(q, ignoreCase = true)) return true
        return matchesIdentity(api, q)
    }

    fun filterGrid(apis: List<MainAPI>, query: String): List<MainAPI> {
        if (query.isBlank()) return apis
        return apis.filter { matchesGridFilter(it, query) }
    }

    fun matchesIdentity(api: MainAPI, query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.length < 3) return false
        val name = api.name.trim().lowercase()
        val host = PluginHubUi.siteHost(api.mainUrl).lowercase().removePrefix("www.")
        if (name.isBlank() && host.isBlank()) return false
        if (name == q || host == q) return true
        if (PluginHubUi.siteMatches(api.mainUrl, q)) return true
        if (name.startsWith(q) || host.startsWith(q)) return true
        // Single-token typos only: "wattap" → Wattpad (not "wattpad romance").
        if (' ' !in q && q.length >= 4 && name.length >= 4) {
            val shared = q.commonPrefixWith(name).length
            val lengthDelta = kotlin.math.abs(q.length - name.length)
            if (shared >= 4 && lengthDelta <= 2) return true
        }
        return false
    }

    /** Removes the provider name/host from the start of [query] when present. */
    fun stripIdentity(query: String, api: MainAPI): String {
        var rest = query.trim()
        val tokens = listOfNotNull(
            api.name.trim().takeIf { it.isNotBlank() },
            PluginHubUi.siteHost(api.mainUrl).removePrefix("www.").takeIf { it.isNotBlank() },
            api.mainUrl.trim().takeIf { it.isNotBlank() },
        ).sortedByDescending { it.length }
        for (token in tokens) {
            if (rest.startsWith(token, ignoreCase = true)) {
                rest = rest.removePrefix(rest.take(token.length)).trimStart(' ', ':', '-', '|')
                break
            }
        }
        // Typo prefix strip (wattap…)
        val name = api.name.trim()
        if (rest.equals(query.trim(), ignoreCase = true) && name.length >= 4) {
            val q = rest.lowercase()
            val n = name.lowercase()
            val shared = q.commonPrefixWith(n).length
            if (shared >= 4 && shared < q.length) {
                // keep as novel query — do not strip partial typos mid-word oddly
            } else if (shared >= 4 && shared == q.length) {
                rest = ""
            }
        }
        return rest
    }
}
