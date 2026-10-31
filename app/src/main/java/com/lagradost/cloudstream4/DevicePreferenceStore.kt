package com.lagradost.cloudstream4

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.lagradost.quicknovel.GeminiProtocol
import com.lagradost.quicknovel.util.Apis.Companion.apis
import com.mihon.common.preference.AndroidPreferenceStore
import com.mihon.common.preference.PreferenceStore
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentHashSet

@Composable
fun rememberAppSettings(): AppSettings {
    val context = LocalContext.current
    val value = remember(context) { AppSettings(context) }
    return value
}

fun AppSettings(context: Context): AppSettings {
    return AppSettings(preferences = AndroidPreferenceStore(context))
}

/**
 * App settings for every setting we have, that way we can just inject AppSettings into a viewmodel,
 * or similar.
 *
 * We use an internal constructor to force the user to construct it using the plantform specific
 * initalizer
 * */
class AppSettings internal constructor(
    preferences: PreferenceStore,
) {
    val ui = UIPreferences(preferences)
    val provider = ProviderPreferences(preferences)
    val updates = UpdatePreferences(preferences)
    val reader = ReaderPreferences(preferences)
    val translation = TranslationPreferences(preferences)
}

class TranslationPreferences(preferences: PreferenceStore) {
    val geminiApiKey = preferences.getString("gemini_api_key", "")
    val geminiModel = preferences.getString("gemini_model", GeminiProtocol.DEFAULT_MODEL)
}

class ReaderPreferences(preferences: PreferenceStore) {
    val externalReader = preferences.getBoolean(
        "external_reader",
        true,
    )
}


class ProviderPreferences(preferences: PreferenceStore) {
    val searchProvidersList =
        preferences.getStringSet("search_providers_list", apis.map { it.name }.toSet())
    val searchLangList =
        preferences.getStringSet("provider_lang_key", persistentSetOf("es"))
    val showEnglishProviders = preferences.getBoolean("show_english_providers", false)
    /** When false (default), search fields strip symbols like @ # $ %. */
    val allowSpecialSearchChars = preferences.getBoolean("allow_special_search_chars", false)
}

class UpdatePreferences(preferences: PreferenceStore) {
    val showAppUpdates = preferences.getBoolean(
        "auto_update",
        false,
    )
    val skipUpdate = preferences.getString("skip_update_key", "")
    val apkInstaller = preferences.getInt("apk_installer_key",1)
    val notifyNewChapters = preferences.getBoolean("notify_new_chapters", true)
    val chapterCheckHours = preferences.getInt("chapter_check_hours", 12)
}

class UIPreferences(preferences: PreferenceStore) {
    val primaryColor = preferences.getString(
        "primary_color_key", "UltimateBlue"
    )
    val theme = preferences.getString(
        "theme_key", "UltimateBlue"
    )
    /** Ultimate surface: true = dark, false = light. */
    val darkMode = preferences.getBoolean(
        "dark_mode_key", true
    )
    val appIconTheme = preferences.getString(
        "app_icon_theme_key", "BlueFire"
    )
    /** Ultimate accent pack (BlueFire, EmeraldFire, …). Colors only. */
    val uiIconTheme = preferences.getString(
        "ui_icon_theme_key", "BlueFire"
    )
    /** Icon treatment A–J; independent from [uiIconTheme]. */
    val iconStyle = preferences.getString(
        "icon_style_key", "A"
    )
    val ratingFormat = preferences.getString(
        "rating_format",
        "star",
    )
    val navStyle = preferences.getString("nav_style", "A")
    val settingsStyle = preferences.getString("settings_style", "1")
    val locale = preferences.getString(
        "locale_key",
        "es",
    )
}