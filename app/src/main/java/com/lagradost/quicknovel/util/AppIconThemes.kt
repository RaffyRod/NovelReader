package com.lagradost.quicknovel.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.preference.PreferenceManager

/**
 * Launcher icon packs: only Blue / Black / Lava aliases exist as APK components.
 * Newer Ultimate packs map onto the closest alias until dedicated icons ship.
 *
 * Component flips are deferred via [PENDING_LAUNCHER_KEY] so changing theme
 * in Settings does not kill the process mid-session (common OEM behavior).
 */
object AppIconThemes {
    const val PREF_KEY = "app_icon_theme_key"
    const val PENDING_LAUNCHER_KEY = "app_icon_theme_pending"
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

    /** Maps any Ultimate pack key onto an existing launcher alias. */
    fun launcherBucket(themeId: String): String = when (themeId) {
        LEGACY_ULTIMATE_BLUE, BLUE_FIRE,
        "EmeraldFire", "CyanFire", "MidnightFire", "SoftFire",
        "UltimateBlue", "UltimateEmerald", "UltimateCyan", "UltimateMidnight", "UltimateSoft",
        -> BLUE_FIRE
        BLACK_FIRE, "UltimateBlack", "White", "Grey",
        -> BLACK_FIRE
        LAVA_FIRE, "GoldFire", "RoseFire", "CrimsonFire",
        "UltimateLava", "UltimateGold", "UltimateRose", "UltimateCrimson",
        "Red", "Party", "Orange",
        -> LAVA_FIRE
        "VioletFire", "UltimateViolet",
        -> BLUE_FIRE
        else -> BLUE_FIRE
    }

    /**
     * Enables the chosen launcher alias and disables the others.
     * Prefer [applyFromPrefs] so pending mid-session changes wait for cold start.
     */
    fun apply(context: Context, themeId: String = BLUE_FIRE) {
        val chosen = launcherBucket(themeId)
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
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(PENDING_LAUNCHER_KEY, false)
            .apply()
    }

    /**
     * Reconciles launcher aliases on cold start so only the chosen icon stays enabled.
     * Mid-session theme flips set [PENDING_LAUNCHER_KEY]; we also repair drift after
     * reinstall/OEM resets even when that flag is clear (multiple LAUNCHER icons).
     * Does not rewrite the stored Ultimate pack key.
     */
    fun applyFromPrefs(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val raw = prefs.getString(PREF_KEY, BLUE_FIRE) ?: BLUE_FIRE
        // Always sync components on cold start — pending only mattered to defer the flip.
        prefs.edit().putBoolean(PENDING_LAUNCHER_KEY, false).apply()
        apply(context, raw)
    }

    fun current(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PREF_KEY, BLUE_FIRE) ?: BLUE_FIRE
    }
}
