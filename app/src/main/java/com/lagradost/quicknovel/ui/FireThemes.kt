package com.lagradost.quicknovel.ui

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.lagradost.quicknovel.CommonActivity
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.util.AppIconThemes

/**
 * Ultimate pack: accents, settings icons, bottom menu, sections rail,
 * and deferred launcher (mapped to Blue / Black / Lava aliases).
 */
object FireThemes {
    fun normalize(key: String?): String = UiIconTheme.fromKey(key).key

    fun primaryKeyFor(fireKey: String): String = UiIconTheme.fromKey(fireKey).primaryKey

    fun overlayStyleFor(fireKey: String, darkSurface: Boolean = true): Int {
        val theme = UiIconTheme.fromKey(fireKey)
        if (!darkSurface) {
            return when (theme) {
                UiIconTheme.BLACK_FIRE -> R.style.OverlayPrimaryColorUltimateBlackLight
                UiIconTheme.SOFT_FIRE -> R.style.OverlayPrimaryColorUltimateSoftLight
                UiIconTheme.GOLD_FIRE -> R.style.OverlayPrimaryColorUltimateGoldLight
                UiIconTheme.CYAN_FIRE -> R.style.OverlayPrimaryColorUltimateCyanLight
                UiIconTheme.EMERALD_FIRE -> R.style.OverlayPrimaryColorUltimateEmeraldLight
                else -> overlayStyleFor(fireKey, darkSurface = true)
            }
        }
        return when (theme) {
            UiIconTheme.BLACK_FIRE -> R.style.OverlayPrimaryColorUltimateBlack
            UiIconTheme.LAVA_FIRE -> R.style.OverlayPrimaryColorUltimateLava
            UiIconTheme.EMERALD_FIRE -> R.style.OverlayPrimaryColorUltimateEmerald
            UiIconTheme.VIOLET_FIRE -> R.style.OverlayPrimaryColorUltimateViolet
            UiIconTheme.GOLD_FIRE -> R.style.OverlayPrimaryColorUltimateGold
            UiIconTheme.CYAN_FIRE -> R.style.OverlayPrimaryColorUltimateCyan
            UiIconTheme.ROSE_FIRE -> R.style.OverlayPrimaryColorUltimateRose
            UiIconTheme.MIDNIGHT_FIRE -> R.style.OverlayPrimaryColorUltimateMidnight
            UiIconTheme.CRIMSON_FIRE -> R.style.OverlayPrimaryColorUltimateCrimson
            UiIconTheme.SOFT_FIRE -> R.style.OverlayPrimaryColorUltimateSoft
            else -> R.style.OverlayPrimaryColorUltimateBlue
        }
    }

    fun current(context: Context): UiIconTheme {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val fromUi = prefs.getString(UiIconTheme.PREF_KEY, null)
        val fromLauncher = prefs.getString(AppIconThemes.PREF_KEY, null)
        return UiIconTheme.fromKey(normalize(fromUi ?: fromLauncher))
    }

    /**
     * Persists Ultimate pack prefs. Launcher alias is deferred to next cold start
     * so OEM kills from setComponentEnabledSetting do not drop the session.
     */
    fun apply(context: Context, key: String, recreateUi: Boolean = true) {
        val theme = UiIconTheme.fromKey(key)
        val dark = AppearanceMode.isDark(context)
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(UiIconTheme.PREF_KEY, theme.key)
            putString(AppIconThemes.PREF_KEY, theme.key)
            putString(
                context.getString(R.string.theme_key),
                if (dark) "UltimateBlue" else "Light",
            )
            putString(context.getString(R.string.primary_color_key), theme.primaryKey)
            putBoolean(AppIconThemes.PENDING_LAUNCHER_KEY, true)
        }
        if (recreateUi) {
            (context as? Activity)?.let { softRecreate(it) }
        }
    }

    /** Migrate stock color prefs into an Ultimate pack; preserve dark/light appearance. */
    fun migrateIfNeeded(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val theme = prefs.getString(context.getString(R.string.theme_key), "UltimateBlue")
        val primary = prefs.getString(context.getString(R.string.primary_color_key), "UltimateBlue")
        val known = UiIconTheme.entries.map { it.primaryKey }.toSet()
        val fireFromPrefs = normalize(
            prefs.getString(UiIconTheme.PREF_KEY, null)
                ?: prefs.getString(AppIconThemes.PREF_KEY, null)
        )
        val fire = when {
            primary in known -> UiIconTheme.fromKey(primary).key
            primary in setOf("White", "Grey") -> UiIconTheme.BLACK_FIRE.key
            primary in setOf("Red", "Party", "Orange") -> UiIconTheme.LAVA_FIRE.key
            else -> fireFromPrefs
        }
        val dark = when (theme) {
            "Light", "Lavender", "AmoledLight" -> false
            else -> prefs.getBoolean(AppearanceMode.PREF_KEY, AppearanceMode.DEFAULT_DARK)
        }
        val needsPrimary = primary !in known
        val needsAppearance = !prefs.contains(AppearanceMode.PREF_KEY)
        if (!needsPrimary && !needsAppearance && fire == fireFromPrefs) return
        prefs.edit {
            putBoolean(AppearanceMode.PREF_KEY, dark)
            putString(
                context.getString(R.string.theme_key),
                if (dark) "UltimateBlue" else "Light",
            )
            putString(context.getString(R.string.primary_color_key), primaryKeyFor(fire))
            putString(UiIconTheme.PREF_KEY, fire)
            putString(AppIconThemes.PREF_KEY, fire)
        }
    }

    fun softRecreate(activity: Activity) {
        CommonActivity.loadThemes(activity)
        activity.overridePendingTransition(0, 0)
        activity.recreate()
        activity.overridePendingTransition(0, 0)
    }
}
