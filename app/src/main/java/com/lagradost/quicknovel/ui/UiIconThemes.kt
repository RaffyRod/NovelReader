package com.lagradost.quicknovel.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Fire-aura packs for settings-row icons and the bottom menu.
 * Independent from the launcher icon theme ([com.lagradost.quicknovel.util.AppIconThemes]).
 */
enum class UiIconTheme(
    val key: String,
    /** Bright glyph color over the fire badge. */
    val glyph: Color,
    /** Core fire / accent. */
    val core: Color,
    /** Soft outer glow. */
    val glow: Color,
    /** Tile gradient pairs used by settings style 1. */
    val tiles: List<List<Color>>,
    /** Android ColorInt for View-based nav tint. */
    val coreArgb: Int,
    val glowArgb: Int,
) {
    BLUE_FIRE(
        key = "BlueFire",
        glyph = Color(0xFFB8DCFF),
        core = Color(0xFF1A6CFF),
        glow = Color(0xFF4DA3FF),
        tiles = listOf(
            listOf(Color(0xFF4DA3FF), Color(0xFF1A6CFF)),
            listOf(Color(0xFF7EC0FF), Color(0xFF1A6CFF)),
            listOf(Color(0xFF1A6CFF), Color(0xFF0A3FA0)),
            listOf(Color(0xFF5AB0FF), Color(0xFF1558D4)),
            listOf(Color(0xFF3D8BFF), Color(0xFF0D4BC4)),
            listOf(Color(0xFF8ECDFF), Color(0xFF1A6CFF)),
        ),
        coreArgb = 0xFF1A6CFF.toInt(),
        glowArgb = 0xFF4DA3FF.toInt(),
    ),
    BLACK_FIRE(
        key = "BlackFire",
        glyph = Color(0xFFF5F7FA),
        core = Color(0xFFE8EEF8),
        glow = Color(0xFFFFFFFF),
        tiles = listOf(
            listOf(Color(0xFF4A5160), Color(0xFF1A1D24)),
            listOf(Color(0xFF6B7384), Color(0xFF2A2F3A)),
            listOf(Color(0xFF3A4050), Color(0xFF12151C)),
            listOf(Color(0xFF8B93A3), Color(0xFF2E3440)),
            listOf(Color(0xFF5C6575), Color(0xFF1E222B)),
            listOf(Color(0xFF9AA3B2), Color(0xFF252A35)),
        ),
        coreArgb = 0xFFE8EEF8.toInt(),
        glowArgb = 0xFFFFFFFF.toInt(),
    ),
    LAVA_FIRE(
        key = "LavaFire",
        glyph = Color(0xFFFFD0B8),
        core = Color(0xFFFF3B1C),
        glow = Color(0xFFFF7A3D),
        tiles = listOf(
            listOf(Color(0xFFFF7A3D), Color(0xFFFF3B1C)),
            listOf(Color(0xFFFF9A6A), Color(0xFFE82A10)),
            listOf(Color(0xFFFF3B1C), Color(0xFFB01A08)),
            listOf(Color(0xFFFF5A3C), Color(0xFFD42812)),
            listOf(Color(0xFFFF8A50), Color(0xFFFF3B1C)),
            listOf(Color(0xFFFF6A2E), Color(0xFFC41E0A)),
        ),
        coreArgb = 0xFFFF3B1C.toInt(),
        glowArgb = 0xFFFF7A3D.toInt(),
    );

    companion object {
        const val PREF_KEY = "ui_icon_theme_key"
        const val DEFAULT_KEY = "BlueFire"

        fun fromKey(key: String?): UiIconTheme {
            val normalized = when (key) {
                "UltimateBlue", "BlueFire" -> BLUE_FIRE.key
                "BlackFire" -> BLACK_FIRE.key
                "LavaFire" -> LAVA_FIRE.key
                else -> key
            }
            return entries.firstOrNull { it.key == normalized } ?: BLUE_FIRE
        }
    }
}

val LocalUiIconTheme = compositionLocalOf { UiIconTheme.BLUE_FIRE }
