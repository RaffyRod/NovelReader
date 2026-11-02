package com.lagradost.quicknovel.plugins

import com.lagradost.quicknovel.BaseApplication.Companion.getKey
import com.lagradost.quicknovel.BaseApplication.Companion.setKey

/** One remembered catalog JSON URL (for “Add repo” autocomplete). */
data class PluginRepoUrlEntry(
    val url: String = "",
    val label: String = "",
    val usedAt: Long = 0L,
)

/** Pure list rules for recent plugin catalog URLs (unit-tested). */
object PluginRepoUrlHistoryLogic {
    const val MAX = 12

    fun add(current: List<PluginRepoUrlEntry>, url: String, label: String = ""): List<PluginRepoUrlEntry> {
        val clean = url.trim()
        if (clean.isBlank()) return current
        val entry = PluginRepoUrlEntry(
            url = clean,
            label = label.trim().ifBlank { hostOf(clean) },
            usedAt = System.currentTimeMillis(),
        )
        return (listOf(entry) + current.filter { !it.url.equals(clean, ignoreCase = true) }).take(MAX)
    }

    fun remove(current: List<PluginRepoUrlEntry>, url: String): List<PluginRepoUrlEntry> =
        current.filter { !it.url.equals(url.trim(), ignoreCase = true) }

    fun clear(): List<PluginRepoUrlEntry> = emptyList()

    fun hostOf(url: String): String =
        url.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .ifBlank { url.trim() }
}

/** Persists recent plugin catalog URLs so Add-repo can autocomplete instead of always prefilling official. */
object PluginRepoUrlHistory {
    private const val KEY = "plugin_repo_url_history"

    fun load(): List<PluginRepoUrlEntry> =
        getKey<Array<PluginRepoUrlEntry>>(KEY)?.toList()
            ?: getKey<List<PluginRepoUrlEntry>>(KEY)
            ?: emptyList()

    fun remember(url: String, label: String = "") {
        setKey(KEY, PluginRepoUrlHistoryLogic.add(load(), url, label).toTypedArray())
    }

    fun remove(url: String) {
        setKey(KEY, PluginRepoUrlHistoryLogic.remove(load(), url).toTypedArray())
    }

    fun clear() {
        setKey(KEY, emptyArray<PluginRepoUrlEntry>())
    }

    /** Last used URL, or null when the user has never fetched a custom catalog. */
    fun lastUrl(): String? = load().firstOrNull()?.url
}
