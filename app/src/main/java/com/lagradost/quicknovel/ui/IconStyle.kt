package com.lagradost.quicknovel.ui

import androidx.compose.runtime.compositionLocalOf

/**
 * Visual treatment for settings / nav icons.
 * Independent from [UiIconTheme] (Ultimate accent color).
 */
enum class IconStyle(val key: String) {
    /** Fire aura behind glyph (default / current look). */
    FIRE_AURA("A"),
    SOFT_HALO("B"),
    FLAT_TINT("C"),
    OUTLINE_RING("D"),
    SQUIRCLE_TILE("E"),
    NEON_EDGE("F"),
    GLASS_CAPSULE("G"),
    DUOTONE_BADGE("H"),
    MINIMAL_GLYPH("I"),
    ORBIT_DOT("J");

    companion object {
        const val PREF_KEY = "icon_style_key"
        const val DEFAULT_KEY = "A"

        fun fromKey(key: String?): IconStyle =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: FIRE_AURA
    }
}

val LocalIconStyle = compositionLocalOf { IconStyle.FIRE_AURA }
