package com.lagradost.quicknovel.plugins

import com.lagradost.quicknovel.BaseApplication.Companion.getKey
import com.lagradost.quicknovel.BaseApplication.Companion.getKeys
import com.lagradost.quicknovel.BaseApplication.Companion.removeKey
import com.lagradost.quicknovel.BaseApplication.Companion.setKey

/** Persistence for plugin repos and installed plugin metadata. */
object PluginStore {
    private const val REPO_FOLDER = "plugin_repo"
    private const val INSTALLED_FOLDER = "plugin_installed"
    private const val CACHE_FOLDER = "plugin_manifest_cache"

    const val OFFICIAL_MANIFEST_URL =
        "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist/plugins.min.json"

    fun loadRepos(): List<PluginRepo> {
        val keys = getKeys(REPO_FOLDER) ?: return emptyList()
        return keys.mapNotNull { key -> getKey<PluginRepo>(key) }.sortedBy { it.label }
    }

    fun saveRepo(repo: PluginRepo) {
        setKey(REPO_FOLDER, repo.id, repo)
    }

    fun removeRepo(id: String) {
        removeKey(REPO_FOLDER, id)
        removeKey(CACHE_FOLDER, id)
    }

    fun loadInstalled(): List<InstalledPlugin> {
        val keys = getKeys(INSTALLED_FOLDER) ?: return emptyList()
        val byId = linkedMapOf<String, InstalledPlugin>()
        for (fullKey in keys) {
            // getKeys returns prefs paths like "plugin_installed/<id>" — skip unrelated prefixes.
            if (!fullKey.startsWith("$INSTALLED_FOLDER/")) continue
            val plugin = getKey<InstalledPlugin>(fullKey) ?: continue
            val id = plugin.id.ifBlank { fullKey.substringAfterLast('/') }
            if (id.isBlank()) continue
            byId[id] = plugin.copy(id = id)
        }
        // Newest installs first so bulk-adds stay visible at the top of the hub.
        return byId.values.sortedWith(
            compareByDescending<InstalledPlugin> { it.installedAt }
                .thenBy { it.localName.lowercase() },
        )
    }

    fun getInstalled(id: String): InstalledPlugin? =
        getKey(INSTALLED_FOLDER, id)

    fun saveInstalled(plugin: InstalledPlugin) {
        val id = plugin.id.trim().ifBlank {
            plugin.jsUrl.ifBlank { plugin.localName }.ifBlank { plugin.name }
                .hashCode().toString()
        }
        setKey(INSTALLED_FOLDER, id, plugin.copy(id = id))
    }

    fun removeInstalled(id: String) {
        removeKey(INSTALLED_FOLDER, id)
    }

    fun cacheManifest(repoId: String, items: List<PluginManifestItem>) {
        setKey(CACHE_FOLDER, repoId, items)
    }

    fun loadCachedManifest(repoId: String): List<PluginManifestItem> {
        return getKey<ArrayList<PluginManifestItem>>(CACHE_FOLDER, repoId)?.toList()
            ?: getKey<List<PluginManifestItem>>(CACHE_FOLDER, repoId)
            ?: emptyList()
    }

    fun ensureOfficialRepo() {
        if (loadRepos().any { it.url == OFFICIAL_MANIFEST_URL }) return
        saveRepo(
            PluginRepo(
                id = "lnreader_official",
                url = OFFICIAL_MANIFEST_URL,
                label = "LNReader official",
            )
        )
    }
}
