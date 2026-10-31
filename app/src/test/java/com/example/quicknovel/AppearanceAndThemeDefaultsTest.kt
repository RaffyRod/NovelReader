package com.lagradost.quicknovel

import com.lagradost.quicknovel.ui.AppearanceMode
import com.lagradost.quicknovel.ui.FireThemes
import com.lagradost.quicknovel.ui.UiIconTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceAndThemeDefaultsTest {
    @Test
    fun appearance_defaultsToDark() {
        assertTrue(AppearanceMode.DEFAULT_DARK)
        assertEquals("dark_mode_key", AppearanceMode.PREF_KEY)
    }

    @Test
    fun ultimateTheme_defaultsToBlueFire() {
        assertEquals("BlueFire", UiIconTheme.DEFAULT_KEY)
        assertEquals(UiIconTheme.BLUE_FIRE, UiIconTheme.fromKey(null))
        assertEquals(UiIconTheme.BLUE_FIRE, UiIconTheme.fromKey("UltimateBlue"))
        assertEquals("UltimateBlue", UiIconTheme.BLUE_FIRE.primaryKey)
    }

    @Test
    fun fireThemes_normalizeUnknown_fallsBackToBlueFire() {
        assertEquals("BlueFire", FireThemes.normalize(null))
        assertEquals("BlueFire", FireThemes.normalize("Nope"))
        assertEquals("LavaFire", FireThemes.normalize("UltimateLava"))
        assertEquals("UltimateBlue", FireThemes.primaryKeyFor("BlueFire"))
    }

    @Test
    fun fireThemes_overlayDarkBlue_isUltimateBlueOverlay() {
        assertEquals(
            R.style.OverlayPrimaryColorUltimateBlue,
            FireThemes.overlayStyleFor("BlueFire", darkSurface = true),
        )
    }
}
