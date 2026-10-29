package com.lagradost.quicknovel.util

import android.content.Context
import androidx.preference.PreferenceManager
import com.lagradost.quicknovel.BaseApplication.Companion.getKey
import com.lagradost.quicknovel.BaseApplication.Companion.getKeys
import com.lagradost.quicknovel.BaseApplication.Companion.removeKey
import com.lagradost.quicknovel.BaseApplication.Companion.setKey
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.providers.CustomProviderConfig

/** Saves the providers the user created and keeps the provider lists up to date. */
object CustomProviders {
    private const val FOLDER = "custom_provider"

    private fun storeKey(name: String) = name.lowercase().hashCode().toString()

    fun load(): List<CustomProviderConfig> {
        val keys = getKeys(FOLDER) ?: return emptyList()
        return keys.mapNotNull { key -> getKey<CustomProviderConfig>(key) }.sortedBy { it.name }
    }

    /** Names that cannot be used because a provider already has them. */
    fun takenNames(ignoring: String? = null): Set<String> =
        Apis.apis.map { it.name }.filter { it != ignoring }.toSet()

    /** Adds or replaces a provider, [replacing] is the name it had before an edit. */
    fun save(context: Context, config: CustomProviderConfig, replacing: String? = null) {
        if (replacing != null && replacing != config.name) {
            removeKey(FOLDER, storeKey(replacing))
        }
        setKey(FOLDER, storeKey(config.name), config)
        Apis.refreshApis()
        enableInSearch(context, config, replacing)
    }

    fun remove(name: String) {
        removeKey(FOLDER, storeKey(name))
        Apis.refreshApis()
    }

    /** New providers start enabled for search, otherwise they would not show up in the results. */
    private fun enableInSearch(context: Context, config: CustomProviderConfig, replacing: String?) {
        val settings = PreferenceManager.getDefaultSharedPreferences(context)

        val providersKey = context.getString(R.string.search_providers_list_key)
        val savedProviders = settings.getStringSet(providersKey, null)
            ?: Apis.apis.map { it.name }.toSet()
        val updatedProviders = savedProviders.toMutableSet()
        replacing?.let { updatedProviders.remove(it) }
        updatedProviders.add(config.name)
        settings.edit().putStringSet(providersKey, updatedProviders).apply()

        val langKey = context.getString(R.string.provider_lang_key)
        settings.getStringSet(langKey, null)?.let { saved ->
            settings.edit().putStringSet(langKey, saved.toMutableSet().apply { add(config.lang) }).apply()
        }
    }
}
