package com.lagradost.quicknovel.ui

import android.content.Context
import androidx.preference.PreferenceManager
import com.lagradost.quicknovel.util.AppIconThemes

/**
 * Single Fire theme (Blue / Black / Lava) shared by launcher, settings icons,
 * bottom menu and library section rail.
 */
object FireThemes {
    fun normalize(key: String?): String = when (key) {
        AppIconThemes.LEGACY_ULTIMATE_BLUE,
        AppIconThemes.BLUE_FIRE,
        UiIconTheme.BLUE_FIRE.key,
        -> UiIconTheme.BLUE_FIRE.key
        AppIconThemes.BLACK_FIRE,
        UiIconTheme.BLACK_FIRE.key,
        -> UiIconTheme.BLACK_FIRE.key
        AppIconThemes.LAVA_FIRE,
        UiIconTheme.LAVA_FIRE.key,
        -> UiIconTheme.LAVA_FIRE.key
        else -> UiIconTheme.DEFAULT_KEY
    }

    fun current(context: Context): UiIconTheme {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val fromUi = prefs.getString(UiIconTheme.PREF_KEY, null)
        val fromLauncher = prefs.getString(AppIconThemes.PREF_KEY, null)
        return UiIconTheme.fromKey(normalize(fromUi ?: fromLauncher))
    }

    /** Persists both prefs, applies launcher aliases. */
    fun apply(context: Context, key: String) {
        val id = normalize(key)
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(UiIconTheme.PREF_KEY, id)
            .putString(AppIconThemes.PREF_KEY, id)
            .apply()
        AppIconThemes.apply(context, id)
    }
}
