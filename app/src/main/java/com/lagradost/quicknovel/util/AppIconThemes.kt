package com.lagradost.quicknovel.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.preference.PreferenceManager

/**
 * Launcher icon packs aligned with menu Fire themes:
 * BlueFire (default), BlackFire, LavaFire.
 * Legacy pref value [LEGACY_ULTIMATE_BLUE] maps to BlueFire.
 */
object AppIconThemes {
    const val PREF_KEY = "app_icon_theme_key"
    const val BLUE_FIRE = "BlueFire"
    const val BLACK_FIRE = "BlackFire"
    const val LAVA_FIRE = "LavaFire"
    /** @deprecated Kept so existing installs keep the blue launcher. */
    const val LEGACY_ULTIMATE_BLUE = "UltimateBlue"
    const val ULTIMATE_BLUE = BLUE_FIRE

    /** Fully-qualified alias class names (manifest package, not applicationId). */
    private val aliases = linkedMapOf(
        BLUE_FIRE to "com.lagradost.quicknovel.icons.UltimateBlueLauncher",
        BLACK_FIRE to "com.lagradost.quicknovel.icons.UltimateBlackLauncher",
        LAVA_FIRE to "com.lagradost.quicknovel.icons.UltimateLavaLauncher",
    )

    private fun normalize(themeId: String): String = when (themeId) {
        LEGACY_ULTIMATE_BLUE, BLUE_FIRE -> BLUE_FIRE
        BLACK_FIRE -> BLACK_FIRE
        LAVA_FIRE -> LAVA_FIRE
        else -> BLUE_FIRE
    }

    /** Enables the chosen launcher alias and disables the others. */
    fun apply(context: Context, themeId: String = BLUE_FIRE) {
        val chosen = normalize(themeId)
        val pm = context.packageManager
        val pkg = context.packageName
        aliases.forEach { (id, className) ->
            val state = if (id == chosen) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(
                ComponentName(pkg, className),
                state,
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    /** Reads the preference (default Blue Fire) and applies it. */
    fun applyFromPrefs(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val raw = prefs.getString(PREF_KEY, BLUE_FIRE) ?: BLUE_FIRE
        val id = normalize(raw)
        if (raw != id) {
            prefs.edit().putString(PREF_KEY, id).apply()
        }
        apply(context, id)
    }

    fun current(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return normalize(prefs.getString(PREF_KEY, BLUE_FIRE) ?: BLUE_FIRE)
    }
}
