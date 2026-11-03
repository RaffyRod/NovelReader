package com.lagradost.quicknovel.util

import android.content.Context
import androidx.preference.PreferenceManager
import com.lagradost.quicknovel.BaseApplication.Companion.getKey
import com.lagradost.quicknovel.BaseApplication.Companion.getKeys
import com.lagradost.quicknovel.BaseApplication.Companion.removeKey
import com.lagradost.quicknovel.BaseApplication.Companion.setKey
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.providers.CustomProviderConfig
import java.nio.charset.StandardCharsets

/** Saves the providers the user created and keeps the provider lists up to date. */
object CustomProviders {
    private const val FOLDER = "custom_provider"

    /**
     * Stable, collision-free preference key for any Unicode [name].
     * Legacy [hashCode] collided; ASCII-only sanitizing collapsed CJK/Arabic names to one key.
     */
    fun storeKey(name: String): String {
        val normalized = name.trim().lowercase()
        if (normalized.isEmpty()) return "provider_empty"
        return normalized.toByteArray(StandardCharsets.UTF_8).joinToString(separator = "") { byte ->
            val v = byte.toInt() and 0xff
            "%02x".format(v)
        }
    }

    /** Previous key schemes — kept so we can migrate/remove old entries. */
    fun legacyStoreKey(name: String): String = name.lowercase().hashCode().toString()

    fun legacyAsciiStoreKey(name: String): String {
        val cleaned = name.trim().lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "_")
            .trim('_')
        return cleaned.ifBlank { "provider" }
    }

    fun load(): List<CustomProviderConfig> {
        val keys = getKeys(FOLDER) ?: return emptyList()
        val byName = linkedMapOf<String, CustomProviderConfig>()
        for (fullKey in keys) {
            // getKeys returns full prefs paths like "custom_provider/<key>"
            if (!fullKey.startsWith("$FOLDER/")) continue
            val config = getKey<CustomProviderConfig>(fullKey) ?: continue
            byName[config.name] = config
        }
        return byName.values.sortedBy { it.name.lowercase() }
    }

    /** Names that cannot be used because a built-in or custom provider already has them. */
    fun takenNames(ignoring: String? = null): Set<String> {
        val names = LinkedHashSet<String>()
        Apis.apis.forEach { names.add(it.name) }
        load().forEach { names.add(it.name) }
        if (ignoring != null) names.remove(ignoring)
        return names
    }

    /** Adds or replaces a provider, [replacing] is the name it had before an edit. */
    fun save(context: Context, config: CustomProviderConfig, replacing: String? = null) {
        if (replacing != null && replacing != config.name) {
            removeKeysForName(replacing)
        }
        removeKeysForName(config.name)
        setKey(FOLDER, storeKey(config.name), config)
        Apis.refreshApis()
        enableInSearch(context, config, replacing)
    }

    fun remove(name: String) {
        removeKeysForName(name)
        Apis.refreshApis()
    }

    /** Removes the custom provider and drops it from the search multi-select. */
    fun remove(context: Context, name: String) {
        remove(name)
        disableInSearch(context, name)
    }

    private fun removeKeysForName(name: String) {
        removeKey(FOLDER, storeKey(name))
        removeKey(FOLDER, legacyStoreKey(name))
        removeKey(FOLDER, legacyAsciiStoreKey(name))
    }

    /**
     * Ensures every saved custom provider is enabled for search and its language is allowed.
     * Call before a novel search so newly added providers are queried even if the UI still
     * holds a stale provider snapshot.
     */
    fun syncEnabledInSearch(context: Context): Set<String> {
        val customs = load()
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val providersKey = context.getString(R.string.search_providers_list_key)
        val savedProviders = HashSet(
            settings.getStringSet(providersKey, null) ?: Apis.apis.map { it.name },
        )
        var providersChanged = false
        for (config in customs) {
            if (savedProviders.add(config.name)) providersChanged = true
        }
        if (providersChanged) {
            settings.edit().putStringSet(providersKey, HashSet(savedProviders)).apply()
        }

        val langKey = context.getString(R.string.provider_lang_key)
        val savedLangs = settings.getStringSet(langKey, null)?.let { HashSet(it) }
        if (savedLangs != null) {
            var langsChanged = false
            for (config in customs) {
                if (config.lang.isNotBlank() && savedLangs.add(config.lang)) langsChanged = true
            }
            if (langsChanged) {
                settings.edit().putStringSet(langKey, HashSet(savedLangs)).apply()
            }
        }
        return savedProviders.toSet()
    }

    /** New providers start enabled for search, otherwise they would not show up in the results. */
    private fun enableInSearch(context: Context, config: CustomProviderConfig, replacing: String?) {
        setSearchEnabled(context, config.name, enabled = true, replacing = replacing)
        val langKey = context.getString(R.string.provider_lang_key)
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        settings.getStringSet(langKey, null)?.let { saved ->
            val langs = HashSet(saved).apply { add(config.lang) }
            settings.edit().putStringSet(langKey, HashSet(langs)).apply()
        }
    }

    private fun disableInSearch(context: Context, name: String) {
        setSearchEnabled(context, name, enabled = false)
    }

    /**
     * Toggles a provider name in the Search multi-select set (Fuentes de Buscar switches).
     */
    fun setSearchEnabled(
        context: Context,
        name: String,
        enabled: Boolean,
        replacing: String? = null,
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val providersKey = context.getString(R.string.search_providers_list_key)
        val updated = HashSet(
            settings.getStringSet(providersKey, null) ?: Apis.apis.map { it.name },
        )
        replacing?.let { updated.remove(it) }
        if (enabled) updated.add(trimmed) else updated.remove(trimmed)
        settings.edit().putStringSet(providersKey, HashSet(updated)).apply()
        com.lagradost.quicknovel.APIRepository.providersActive = HashSet(updated)
    }
}
