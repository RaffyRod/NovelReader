package com.lagradost.quicknovel.plugins

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/** One entry from an LNReader-style `plugins.min.json` manifest (catalog only). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PluginManifestItem(
    @JsonProperty("id") val id: String = "",
    @JsonProperty("name") val name: String = "",
    @JsonProperty("site") val site: String = "",
    @JsonProperty("lang") val lang: String = "",
    @JsonProperty("version") val version: String = "",
    @JsonProperty("url") val url: String = "",
    @JsonProperty("iconUrl") val iconUrl: String = "",
    @JsonProperty("customJS") val customJS: String? = null,
    @JsonProperty("customCSS") val customCSS: String? = null,
) {
    val displayLang: String get() = lang.trim().trimStart('\u200e')
}

/** A remote (or pasted) catalog the user added. */
data class PluginRepo(
    val id: String,
    val url: String,
    val label: String = "LNReader",
    val lastSyncedAt: Long = 0L,
    val pluginCount: Int = 0,
)

/**
 * A plugin the user installed into Ultimate.
 * Runtime for search is a [com.lagradost.quicknovel.providers.CustomProviderConfig]
 * derived from the site URL / multisrc hint; the JS file is kept for update/reinstall.
 */
data class InstalledPlugin(
    val id: String,
    val name: String,
    val localName: String = name,
    val site: String = "",
    val lang: String = "",
    val langCode: String = "en",
    val version: String = "",
    val latestVersion: String = version,
    val jsUrl: String = "",
    val iconUrl: String = "",
    val repoUrl: String = "",
    val providerName: String = localName,
    val installedAt: Long = System.currentTimeMillis(),
    val jsPath: String = "",
) {
    val hasUpdate: Boolean
        get() = latestVersion.isNotBlank() && latestVersion != version
}
