package com.lagradost.quicknovel.ui

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.lagradost.quicknovel.R

/**
 * Dark / light surface mode. Independent from Ultimate accent packs and icon styles.
 */
object AppearanceMode {
    const val PREF_KEY = "dark_mode_key"
    const val DEFAULT_DARK = true

    fun isDark(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PREF_KEY, DEFAULT_DARK)

    fun apply(context: Context, dark: Boolean, recreateUi: Boolean = true) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(PREF_KEY, dark)
            // Keep legacy theme_key in sync for any remaining readers.
            putString(
                context.getString(R.string.theme_key),
                if (dark) "UltimateBlue" else "Light",
            )
        }
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO,
        )
        if (recreateUi) {
            (context as? Activity)?.let { FireThemes.softRecreate(it) }
        }
    }

    fun syncNightMode(context: Context) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDark(context)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO,
        )
    }

    fun surfaceStyleRes(context: Context): Int =
        if (isDark(context)) R.style.UltimateBlueMode else R.style.UltimateLightMode
}
