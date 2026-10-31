package com.lagradost.cloudstream4.theme

import androidx.compose.ui.graphics.Color

enum class CloudStreamPrimaryColor(val color: Color) {
    ULTIMATE_BLUE(Color(0xFF1A6CFF)),
    ULTIMATE_BLACK(Color(0xFFE8EEF8)),
    ULTIMATE_BLACK_LIGHT(Color(0xFF2A3142)),
    ULTIMATE_LAVA(Color(0xFFFF3B1C)),
    ULTIMATE_EMERALD(Color(0xFF00C853)),
    ULTIMATE_EMERALD_LIGHT(Color(0xFF00A843)),
    ULTIMATE_VIOLET(Color(0xFF7C4DFF)),
    ULTIMATE_GOLD(Color(0xFFFFB300)),
    ULTIMATE_GOLD_LIGHT(Color(0xFFC68400)),
    ULTIMATE_CYAN(Color(0xFF00E5FF)),
    ULTIMATE_CYAN_LIGHT(Color(0xFF00838F)),
    ULTIMATE_ROSE(Color(0xFFFF4081)),
    ULTIMATE_MIDNIGHT(Color(0xFF536DFE)),
    ULTIMATE_CRIMSON(Color(0xFFFF1744)),
    ULTIMATE_SOFT(Color(0xFF90CAF9)),
    ULTIMATE_SOFT_LIGHT(Color(0xFF1565C0)),
    NORMAL(Color(0xFF1A6CFF)),
    BLUE(Color(0xFF5664B7)),
    PURPLE(Color(0xFF6200EA)),
    GREEN(Color(0xFF00BFA5)),
    GREEN_APPLE(Color(0xFF48E484)),
    RED(Color(0xFFD50000)),
    BANANA(Color(0xFFE4D448)),
    PARTY(Color(0xFFEA596E)),
    PINK(Color(0xFFFF1493)),
    CARNATION_PINK(Color(0xFFBD5DA5)),
    MAROON(Color(0xFF451010)),
    DARK_GREEN(Color(0xFF004500)),
    NAVY_BLUE(Color(0xFF000080)),
    GREY(Color(0xFF515151)),
    WHITE(Color(0xFFFFFFFF)),
    BROWN(Color(0xFF622C00)),
    ORANGE(Color(0xFFCE8500)),
    DANDELION_YELLOW(Color(0xFFF5BB00)),
    COOL_BLUE(Color(0xFF408CAC)),
    LAVENDER(Color(0xFF6F55AF)),
    DYNAMIC(Color(0xFFE5322D)),
    DYNAMIC_TWO(Color(0xFFE5322D)),
}


internal object CloudStreamPalette {
    // Default dark / Ultimate Blue brand
    val Primary = Color(0xFF1A6CFF)
    val PrimaryDark = Color(0xFF0A2A6E)
    val Ongoing = Color(0xFF5AA2FF)

    val DarkPrimaryGrayBg = Color(0xFF121A2C)
    val DarkBlackBg = Color(0xFF050B18)
    val DarkIconGrayBg = Color(0xFF0A1224)
    val DarkBoxItemBg = Color(0xFF0D1528)
    val DarkText = Color(0xFFE8F1FF)
    val DarkGrayText = Color(0xFF8B97AB)
    val DarkIcon = Color(0xFF9BA0A6)

    // Ultimate Blue (icon G): pure black + bright blue
    val UltimateBluePrimaryGrayBg = Color(0xFF162038)
    val UltimateBlueBlackBg = Color(0xFF000000)
    val UltimateBlueIconGrayBg = Color(0xFF0A1224)
    val UltimateBlueBoxItemBg = Color(0xFF101A30)
    val UltimateBlueText = Color(0xFFE8F1FF)
    val UltimateBlueGrayText = Color(0xFF8B97AB)
    val UltimateBlueIcon = Color(0xFF9EC5FF)

    // Amoled
    val AmoledBlack = Color(0xFF000000)
    val AmoledNearBlack = Color(0xFF121212)

    // Light (Ultimate-tinted)
    val LightPrimaryGrayBg = Color(0xFFE8EDF5)
    val LightBlackBg = Color(0xFFFFFFFF)
    val LightIconGrayBg = Color(0xFFF1F4FA)
    val LightBoxItemBg = Color(0xFFFFFFFF)
    val LightText = Color(0xFF0F1419)
    val LightGrayText = Color(0xFF4A5568)
    val LightIcon = Color(0xFF2A3142)

    // Dracula
    val DraculaPrimaryGrayBg = Color(0xFF414450)
    val DraculaBlackBg = Color(0xFF282A36)
    val DraculaIconGrayBg = Color(0xFF44475A)
    val DraculaBoxItemBg = Color(0xFF373844)
    val DraculaText = Color(0xFFF8F8F2)
    val DraculaGrayText = Color(0xFF6272A4)
    val DraculaIcon = Color(0xFF6272A4)

    // Lavender Dreams
    val LavenderPrimaryGrayBg = Color(0xFFF7EEFC)
    val LavenderBlackBg = Color(0xFFFDF0FB)
    val LavenderIconGrayBg = Color(0xFFB794F6)
    val LavenderBoxItemBg = Color(0xFFF8F5FF)
    val LavenderText = Color(0xFF2D1B47)
    val LavenderGrayText = Color(0xFF9AB3FF)
    val LavenderIcon = Color(0xFF7C3AED)

    // Silent Blue
    val SilentBluePrimaryGrayBg = Color(0xFF282F49)
    val SilentBlueBlackBg = Color(0xFF151A30)
    val SilentBlueIconGrayBg = Color(0xFF3A446A)
    val SilentBlueBoxItemBg = Color(0xFF3A446A)
    val SilentBlueText = Color(0xFFE0E1F3)
    val SilentBlueGrayText = Color(0xFF7B83B0)
    val SilentBlueIcon = Color(0xFF7B83B0)
}
