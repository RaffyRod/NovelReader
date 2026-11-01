package com.lagradost.quicknovel.plugins

import android.content.Context
import com.lagradost.quicknovel.MainActivity.Companion.app
import com.lagradost.quicknovel.providers.CustomProviderConfig
import com.lagradost.quicknovel.providers.CustomProviderPresets
import com.lagradost.quicknovel.util.CustomProviders
import java.io.File

/**
 * Installs a catalog plugin into Ultimate.
 *
 * Madara / Blogger CSS templates are applied **only** when the plugin is tagged as such
 * (e.g. `[madara]` in the JS URL). Other LNReader JS plugins keep their JS on disk and
 * do **not** get Madara selectors invented for them.
 */
object PluginInstaller {

    /**
     * Builds a CSS config for search. Prefer [jsSource] hints from the downloaded plugin file.
     * Returns null when Ultimate cannot map the plugin to a CSS template (JS-only).
     */
    fun configFromManifest(
        item: PluginManifestItem,
        localName: String? = null,
        jsSource: String = "",
    ): CustomProviderConfig? {
        val name = (localName ?: item.name).trim().ifBlank { item.id }
        val base = CustomProviderPresets.normalizeBaseUrl(item.site)
            ?: item.site.trim().trimEnd('/')
        val lang = PluginLangs.codeFor(item.displayLang)
        return when (PluginTemplateHints.detect(item, jsSource)) {
            PluginTemplateKind.BLOGGER ->
                CustomProviderConfig.bloggerTemplate(name, base).copy(lang = lang)
            PluginTemplateKind.MADARA ->
                CustomProviderConfig.madaraTemplate(name, base).copy(lang = lang)
            PluginTemplateKind.UNSUPPORTED_JS -> null
        }
    }

    suspend fun downloadJs(context: Context, item: PluginManifestItem): Pair<String, String> {
        if (item.url.isBlank()) return "" to ""
        val dir = File(context.filesDir, "plugins").apply { mkdirs() }
        val safeId = item.id.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val out = File(dir, "$safeId.js")
        val text = app.get(item.url).text
        out.writeText(text)
        return out.absolutePath to text
    }

    /**
     * Installs [item] (or updates an existing install). Returns the saved [InstalledPlugin].
     *
     * Existing CustomProvider selectors are preserved on update/rename so a reinstall does not
     * wipe user (or previously correct) configuration with Madara defaults.
     */
    suspend fun install(
        context: Context,
        item: PluginManifestItem,
        repoUrl: String,
        localName: String? = null,
        forceRetemplate: Boolean = false,
    ): InstalledPlugin {
        val existing = PluginStore.getInstalled(item.id)
        val name = localName?.trim()?.takeIf { it.isNotBlank() }
            ?: existing?.localName
            ?: item.name
        val uniqueName = uniqueProviderName(name, ignoring = existing?.providerName)

        val (jsPath, jsText) = runCatching { downloadJs(context, item) }
            .getOrElse {
                val path = existing?.jsPath.orEmpty()
                path to path.takeIf { it.isNotBlank() }?.let { File(it).takeIf { f -> f.isFile }?.readText() }.orEmpty()
            }

        val previousConfig = existing?.providerName
            ?.let { prev -> CustomProviders.load().firstOrNull { it.name == prev } }
        val detected = PluginTemplateHints.detect(item, jsText)
        val freshConfig = configFromManifest(item, uniqueName, jsText)

        // Keep prior CSS only when this install still maps to a CSS template. If detect says
        // JS-only, drop a previously auto-applied Madara/Blogger config so every provider
        // is not stuck with the same wp-manga query.
        val config = when {
            detected == PluginTemplateKind.UNSUPPORTED_JS -> null
            previousConfig != null && !forceRetemplate && freshConfig != null ->
                previousConfig.copy(
                    name = uniqueName,
                    baseUrl = CustomProviderPresets.normalizeBaseUrl(item.site)
                        ?: item.site.trim().trimEnd('/').ifBlank { previousConfig.baseUrl },
                    lang = PluginLangs.codeFor(item.displayLang).ifBlank { previousConfig.lang },
                )
            else -> freshConfig
        }

        if (config != null) {
            CustomProviders.save(context, config, replacing = existing?.providerName)
        } else if (existing?.providerName != null) {
            CustomProviders.remove(existing.providerName)
        }

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
            providerName = if (config != null) uniqueName else "",
            jsPath = jsPath,
            installedAt = existing?.installedAt ?: System.currentTimeMillis(),
            cssMapped = config != null,
        )
        PluginStore.saveInstalled(installed)
        return installed
    }

    fun uninstall(context: Context, plugin: InstalledPlugin) {
        if (plugin.providerName.isNotBlank()) {
            CustomProviders.remove(plugin.providerName)
        }
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
        return install(
            context,
            item.copy(version = item.version.ifBlank { plugin.latestVersion }),
            plugin.repoUrl,
            plugin.localName,
            forceRetemplate = false,
        )
    }

    private fun uniqueProviderName(desired: String, ignoring: String?): String {
        val taken = CustomProviders.takenNames(ignoring = ignoring)
        if (desired !in taken) return desired
        var i = 2
        while ("$desired $i" in taken) i++
        return "$desired $i"
    }
}
