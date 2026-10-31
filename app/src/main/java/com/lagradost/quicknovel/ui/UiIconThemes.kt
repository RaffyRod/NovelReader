package com.lagradost.quicknovel.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Ultimate accent packs for settings-row icons and the bottom menu.
 * Launcher aliases stay Blue/Black/Lava; see [com.lagradost.quicknovel.util.AppIconThemes].
 */
enum class UiIconTheme(
    val key: String,
    val primaryKey: String,
    val glyph: Color,
    val core: Color,
    val glow: Color,
    val tiles: List<List<Color>>,
    val coreArgb: Int,
    val glowArgb: Int,
) {
    BLUE_FIRE(
        key = "BlueFire",
        primaryKey = "UltimateBlue",
        glyph = Color(0xFFB8DCFF),
        core = Color(0xFF1A6CFF),
        glow = Color(0xFF4DA3FF),
        tiles = tilesOf(0xFF4DA3FF, 0xFF1A6CFF, 0xFF0A3FA0),
        coreArgb = 0xFF1A6CFF.toInt(),
        glowArgb = 0xFF4DA3FF.toInt(),
    ),
    BLACK_FIRE(
        key = "BlackFire",
        primaryKey = "UltimateBlack",
        glyph = Color(0xFFF5F7FA),
        core = Color(0xFFE8EEF8),
        glow = Color(0xFFFFFFFF),
        tiles = tilesOf(0xFF8B93A3, 0xFFE8EEF8, 0xFF1A1D24),
        coreArgb = 0xFFE8EEF8.toInt(),
        glowArgb = 0xFFFFFFFF.toInt(),
    ),
    LAVA_FIRE(
        key = "LavaFire",
        primaryKey = "UltimateLava",
        glyph = Color(0xFFFFD0B8),
        core = Color(0xFFFF3B1C),
        glow = Color(0xFFFF7A3D),
        tiles = tilesOf(0xFFFF7A3D, 0xFFFF3B1C, 0xFFB01A08),
        coreArgb = 0xFFFF3B1C.toInt(),
        glowArgb = 0xFFFF7A3D.toInt(),
    ),
    EMERALD_FIRE(
        key = "EmeraldFire",
        primaryKey = "UltimateEmerald",
        glyph = Color(0xFFB9F6CA),
        core = Color(0xFF00C853),
        glow = Color(0xFF69F0AE),
        tiles = tilesOf(0xFF69F0AE, 0xFF00C853, 0xFF007E33),
        coreArgb = 0xFF00C853.toInt(),
        glowArgb = 0xFF69F0AE.toInt(),
    ),
    VIOLET_FIRE(
        key = "VioletFire",
        primaryKey = "UltimateViolet",
        glyph = Color(0xFFE8DDFF),
        core = Color(0xFF7C4DFF),
        glow = Color(0xFFB388FF),
        tiles = tilesOf(0xFFB388FF, 0xFF7C4DFF, 0xFF4527A0),
        coreArgb = 0xFF7C4DFF.toInt(),
        glowArgb = 0xFFB388FF.toInt(),
    ),
    GOLD_FIRE(
        key = "GoldFire",
        primaryKey = "UltimateGold",
        glyph = Color(0xFFFFF3C4),
        core = Color(0xFFFFB300),
        glow = Color(0xFFFFD54F),
        tiles = tilesOf(0xFFFFD54F, 0xFFFFB300, 0xFFC68400),
        coreArgb = 0xFFFFB300.toInt(),
        glowArgb = 0xFFFFD54F.toInt(),
    ),
    CYAN_FIRE(
        key = "CyanFire",
        primaryKey = "UltimateCyan",
        glyph = Color(0xFFE0FFFF),
        core = Color(0xFF00E5FF),
        glow = Color(0xFF84FFFF),
        tiles = tilesOf(0xFF84FFFF, 0xFF00E5FF, 0xFF00838F),
        coreArgb = 0xFF00E5FF.toInt(),
        glowArgb = 0xFF84FFFF.toInt(),
    ),
    ROSE_FIRE(
        key = "RoseFire",
        primaryKey = "UltimateRose",
        glyph = Color(0xFFFFD6E7),
        core = Color(0xFFFF4081),
        glow = Color(0xFFFF80AB),
        tiles = tilesOf(0xFFFF80AB, 0xFFFF4081, 0xFFC51162),
        coreArgb = 0xFFFF4081.toInt(),
        glowArgb = 0xFFFF80AB.toInt(),
    ),
    MIDNIGHT_FIRE(
        key = "MidnightFire",
        primaryKey = "UltimateMidnight",
        glyph = Color(0xFFD6DCFF),
        core = Color(0xFF536DFE),
        glow = Color(0xFF8C9EFF),
        tiles = tilesOf(0xFF8C9EFF, 0xFF536DFE, 0xFF304FFE),
        coreArgb = 0xFF536DFE.toInt(),
        glowArgb = 0xFF8C9EFF.toInt(),
    ),
    CRIMSON_FIRE(
        key = "CrimsonFire",
        primaryKey = "UltimateCrimson",
        glyph = Color(0xFFFFCDD2),
        core = Color(0xFFFF1744),
        glow = Color(0xFFFF5252),
        tiles = tilesOf(0xFFFF5252, 0xFFFF1744, 0xFFD50000),
        coreArgb = 0xFFFF1744.toInt(),
        glowArgb = 0xFFFF5252.toInt(),
    ),
    SOFT_FIRE(
        key = "SoftFire",
        primaryKey = "UltimateSoft",
        glyph = Color(0xFFE3F2FD),
        core = Color(0xFF90CAF9),
        glow = Color(0xFFBBDEFB),
        tiles = tilesOf(0xFFBBDEFB, 0xFF90CAF9, 0xFF42A5F5),
        coreArgb = 0xFF90CAF9.toInt(),
        glowArgb = 0xFFBBDEFB.toInt(),
    );

    companion object {
        const val PREF_KEY = "ui_icon_theme_key"
        const val DEFAULT_KEY = "BlueFire"

        fun fromKey(key: String?): UiIconTheme {
            val normalized = when (key) {
                "UltimateBlue", "BlueFire" -> BLUE_FIRE.key
                "UltimateBlack", "BlackFire", "White" -> BLACK_FIRE.key
                "UltimateLava", "LavaFire", "Red" -> LAVA_FIRE.key
                "UltimateEmerald", "EmeraldFire" -> EMERALD_FIRE.key
                "UltimateViolet", "VioletFire" -> VIOLET_FIRE.key
                "UltimateGold", "GoldFire" -> GOLD_FIRE.key
                "UltimateCyan", "CyanFire" -> CYAN_FIRE.key
                "UltimateRose", "RoseFire" -> ROSE_FIRE.key
                "UltimateMidnight", "MidnightFire" -> MIDNIGHT_FIRE.key
                "UltimateCrimson", "CrimsonFire" -> CRIMSON_FIRE.key
                "UltimateSoft", "SoftFire" -> SOFT_FIRE.key
                else -> key
            }
            return entries.firstOrNull { it.key == normalized } ?: BLUE_FIRE
        }
    }
}

private fun tilesOf(a: Long, b: Long, c: Long): List<List<Color>> = listOf(
    listOf(Color(a), Color(b)),
    listOf(Color(a), Color(c)),
    listOf(Color(b), Color(c)),
    listOf(Color(a), Color(b)),
    listOf(Color(b), Color(a)),
    listOf(Color(a), Color(c)),
)

/**
 * Accents that are pale on dark UI become near-invisible on light surfaces.
 * These companions keep contrast when [AppearanceMode] is light.
 */
fun UiIconTheme.coreFor(darkSurface: Boolean): Color {
    if (darkSurface) return core
    return when (this) {
        UiIconTheme.BLACK_FIRE -> Color(0xFF2A3142)
        UiIconTheme.SOFT_FIRE -> Color(0xFF1565C0)
        UiIconTheme.GOLD_FIRE -> Color(0xFFC68400)
        UiIconTheme.CYAN_FIRE -> Color(0xFF00838F)
        UiIconTheme.EMERALD_FIRE -> Color(0xFF00A843)
        else -> core
    }
}

fun UiIconTheme.glowFor(darkSurface: Boolean): Color {
    if (darkSurface) return glow
    return when (this) {
        UiIconTheme.BLACK_FIRE -> Color(0xFF5C6578)
        UiIconTheme.SOFT_FIRE -> Color(0xFF42A5F5)
        UiIconTheme.GOLD_FIRE -> Color(0xFFFFB300)
        UiIconTheme.CYAN_FIRE -> Color(0xFF00BCD4)
        UiIconTheme.EMERALD_FIRE -> Color(0xFF00C853)
        else -> glow
    }
}

fun UiIconTheme.glyphFor(darkSurface: Boolean): Color {
    if (darkSurface) return glyph
    return when (this) {
        UiIconTheme.BLACK_FIRE -> Color(0xFFFFFFFF)
        UiIconTheme.SOFT_FIRE -> Color(0xFFE3F2FD)
        else -> glyph
    }
}

fun UiIconTheme.coreArgbFor(darkSurface: Boolean): Int = coreFor(darkSurface).toArgb()

fun UiIconTheme.glowArgbFor(darkSurface: Boolean): Int = glowFor(darkSurface).toArgb()

val LocalUiIconTheme = compositionLocalOf { UiIconTheme.BLUE_FIRE }
