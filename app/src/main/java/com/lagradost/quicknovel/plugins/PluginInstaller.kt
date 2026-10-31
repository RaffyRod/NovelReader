package com.lagradost.quicknovel.plugins

import android.content.Context
import com.lagradost.quicknovel.MainActivity.Companion.app
import com.lagradost.quicknovel.providers.CustomProviderConfig
import com.lagradost.quicknovel.providers.CustomProviderPresets
import com.lagradost.quicknovel.util.CustomProviders
import java.io.File

/**
 * Installs a manifest plugin into Ultimate as a CustomProvider (Madara/CSS templates)
 * and keeps the compiled JS on disk for updates / reinstall.
 *
 * Full JS runtime (LNReader-style eval) is not executed here; the site URL drives the CSS provider.
 */
object PluginInstaller {

    fun configFromManifest(item: PluginManifestItem, localName: String? = null): CustomProviderConfig {
        val name = (localName ?: item.name).trim().ifBlank { item.id }
        val base = CustomProviderPresets.normalizeBaseUrl(item.site)
            ?: item.site.trim().trimEnd('/')
        val lang = PluginLangs.codeFor(item.displayLang)
        val hint = (item.url + " " + item.id).lowercase()
        return when {
            "blogger" in hint || "blogspot" in base.lowercase() ->
                CustomProviderConfig.bloggerTemplate(name, base).copy(lang = lang)
            else ->
                // Madara / lightnovelwp / most LNReader multisrc map well to Madara selectors.
                CustomProviderConfig.madaraTemplate(name, base).copy(lang = lang)
        }
    }

    suspend fun downloadJs(context: Context, item: PluginManifestItem): String {
        if (item.url.isBlank()) return ""
        val dir = File(context.filesDir, "plugins").apply { mkdirs() }
        val safeId = item.id.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val out = File(dir, "$safeId.js")
        val text = app.get(item.url).text
        out.writeText(text)
        return out.absolutePath
    }

    /**
     * Installs [item] (or updates an existing install). Returns the saved [InstalledPlugin].
     */
    suspend fun install(
        context: Context,
        item: PluginManifestItem,
        repoUrl: String,
        localName: String? = null,
    ): InstalledPlugin {
        val existing = PluginStore.getInstalled(item.id)
        val name = localName?.trim()?.takeIf { it.isNotBlank() }
            ?: existing?.localName
            ?: item.name
        val uniqueName = uniqueProviderName(name, ignoring = existing?.providerName)
        val config = configFromManifest(item, uniqueName)
        val jsPath = runCatching { downloadJs(context, item) }.getOrDefault(existing?.jsPath.orEmpty())

        CustomProviders.save(context, config, replacing = existing?.providerName)

        val installed = InstalledPlugin(
            id = item.id,
            name = item.name,
            localName = uniqueName,
            site = item.site,
            lang = item.displayLang,
            langCode = PluginLangs.codeFor(item.displayLang),
            version = item.version,
            latestVersion = item.version,
            jsUrl = item.url,
            iconUrl = item.iconUrl,
            repoUrl = repoUrl,
            providerName = uniqueName,
            jsPath = jsPath,
            installedAt = existing?.installedAt ?: System.currentTimeMillis(),
        )
        PluginStore.saveInstalled(installed)
        return installed
    }

    fun uninstall(context: Context, plugin: InstalledPlugin) {
        CustomProviders.remove(plugin.providerName)
        if (plugin.jsPath.isNotBlank()) {
            runCatching { File(plugin.jsPath).delete() }
        }
        PluginStore.removeInstalled(plugin.id)
    }

    /**
     * Refreshes [latestVersion] from [catalog] for installed plugins of [repoUrl].
     */
    fun markUpdatesFromCatalog(catalog: List<PluginManifestItem>, repoUrl: String) {
        val byId = catalog.associateBy { it.id }
        for (installed in PluginStore.loadInstalled().filter { it.repoUrl == repoUrl || repoUrl.isBlank() }) {
            val remote = byId[installed.id] ?: continue
            if (remote.version != installed.latestVersion || remote.version != installed.version) {
                PluginStore.saveInstalled(
                    installed.copy(
                        latestVersion = remote.version,
                        jsUrl = remote.url.ifBlank { installed.jsUrl },
                        iconUrl = remote.iconUrl.ifBlank { installed.iconUrl },
                        site = remote.site.ifBlank { installed.site },
                    )
                )
            }
        }
    }

    suspend fun update(context: Context, plugin: InstalledPlugin, catalogItem: PluginManifestItem? = null): InstalledPlugin {
        val item = catalogItem ?: PluginManifestItem(
            id = plugin.id,
            name = plugin.name,
            site = plugin.site,
            lang = plugin.lang,
            version = plugin.latestVersion.ifBlank { plugin.version },
            url = plugin.jsUrl,
            iconUrl = plugin.iconUrl,
        )
        return install(context, item.copy(version = item.version.ifBlank { plugin.latestVersion }), plugin.repoUrl, plugin.localName)
    }

    private fun uniqueProviderName(desired: String, ignoring: String?): String {
        val taken = CustomProviders.takenNames(ignoring = ignoring)
        if (desired !in taken) return desired
        var i = 2
        while ("$desired $i" in taken) i++
        return "$desired $i"
    }
}
