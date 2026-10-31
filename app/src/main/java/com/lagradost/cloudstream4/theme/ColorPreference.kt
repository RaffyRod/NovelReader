package com.lagradost.cloudstream4.theme

import android.os.Build

/** TODO: This should be replaced with a better system */

fun perfToMode(perf: String?) =
    when (perf) {
        "System" -> CloudStreamThemeMode.FollowSystem
        "UltimateBlue" -> CloudStreamThemeMode.UltimateBlue
        "Black" -> CloudStreamThemeMode.Dark
        "Light" -> CloudStreamThemeMode.Light
        "Amoled" -> CloudStreamThemeMode.Amoled
        "AmoledLight" -> CloudStreamThemeMode.AmoledLight
        "Dracula" -> CloudStreamThemeMode.Dracula
        "Lavender" -> CloudStreamThemeMode.Lavender
        "SilentBlue" -> CloudStreamThemeMode.SilentBlue
        "Monet" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            CloudStreamThemeMode.Dynamic
        } else {
            CloudStreamThemeMode.UltimateBlue
        }

        else -> CloudStreamThemeMode.UltimateBlue
    }

fun perfToColor(perf: String?, darkSurface: Boolean = true) = when (perf) {
    "UltimateBlue", "BlueFire" -> CloudStreamPrimaryColor.ULTIMATE_BLUE
    "UltimateBlack", "BlackFire", "White" ->
        if (darkSurface) CloudStreamPrimaryColor.ULTIMATE_BLACK
        else CloudStreamPrimaryColor.ULTIMATE_BLACK_LIGHT
    "UltimateLava", "LavaFire", "Red" -> CloudStreamPrimaryColor.ULTIMATE_LAVA
    "UltimateEmerald", "EmeraldFire" ->
        if (darkSurface) CloudStreamPrimaryColor.ULTIMATE_EMERALD
        else CloudStreamPrimaryColor.ULTIMATE_EMERALD_LIGHT
    "UltimateViolet", "VioletFire" -> CloudStreamPrimaryColor.ULTIMATE_VIOLET
    "UltimateGold", "GoldFire" ->
        if (darkSurface) CloudStreamPrimaryColor.ULTIMATE_GOLD
        else CloudStreamPrimaryColor.ULTIMATE_GOLD_LIGHT
    "UltimateCyan", "CyanFire" ->
        if (darkSurface) CloudStreamPrimaryColor.ULTIMATE_CYAN
        else CloudStreamPrimaryColor.ULTIMATE_CYAN_LIGHT
    "UltimateRose", "RoseFire" -> CloudStreamPrimaryColor.ULTIMATE_ROSE
    "UltimateMidnight", "MidnightFire" -> CloudStreamPrimaryColor.ULTIMATE_MIDNIGHT
    "UltimateCrimson", "CrimsonFire" -> CloudStreamPrimaryColor.ULTIMATE_CRIMSON
    "UltimateSoft", "SoftFire" ->
        if (darkSurface) CloudStreamPrimaryColor.ULTIMATE_SOFT
        else CloudStreamPrimaryColor.ULTIMATE_SOFT_LIGHT
    "Normal" -> CloudStreamPrimaryColor.NORMAL
    "Blue" -> CloudStreamPrimaryColor.BLUE
    "Purple" -> CloudStreamPrimaryColor.PURPLE
    "Green" -> CloudStreamPrimaryColor.GREEN
    "GreenApple" -> CloudStreamPrimaryColor.GREEN_APPLE
    "Red" -> CloudStreamPrimaryColor.RED
    "Banana" -> CloudStreamPrimaryColor.BANANA
    "Party" -> CloudStreamPrimaryColor.PARTY
    "Pink" -> CloudStreamPrimaryColor.PINK
    "CarnationPink" -> CloudStreamPrimaryColor.CARNATION_PINK
    "Maroon" -> CloudStreamPrimaryColor.MAROON
    "DarkGreen" -> CloudStreamPrimaryColor.DARK_GREEN
    "NavyBlue" -> CloudStreamPrimaryColor.NAVY_BLUE
    "Grey" -> CloudStreamPrimaryColor.GREY
    "White" -> CloudStreamPrimaryColor.WHITE
    "Brown" -> CloudStreamPrimaryColor.BROWN
    "Orange" -> CloudStreamPrimaryColor.ORANGE
    "DandelionYellow" -> CloudStreamPrimaryColor.DANDELION_YELLOW
    "CoolBlue" -> CloudStreamPrimaryColor.COOL_BLUE
    "Lavender" -> CloudStreamPrimaryColor.LAVENDER
    "Monet" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        CloudStreamPrimaryColor.DYNAMIC
    } else {
        CloudStreamPrimaryColor.ULTIMATE_BLUE
    }

    "Monet2" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        CloudStreamPrimaryColor.DYNAMIC_TWO
    } else {
        CloudStreamPrimaryColor.ULTIMATE_BLUE
    }

    else -> CloudStreamPrimaryColor.ULTIMATE_BLUE
}
