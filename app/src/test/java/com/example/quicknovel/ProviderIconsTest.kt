package com.example.quicknovel

import com.lagradost.quicknovel.util.ProviderIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderIconsTest {
    @Test
    fun initials_fromNameParts() {
        assertEquals("LN", ProviderIcons.initials("Light Novel Daily"))
        assertEquals("SK", ProviderIcons.initials("SkyNovels"))
        assertEquals("A", ProviderIcons.initials("A"))
        assertEquals("AB", ProviderIcons.initials("ab"))
        assertEquals("?", ProviderIcons.initials("   "))
    }

    @Test
    fun initials_twoWordTakesFirstLetters() {
        assertEquals("MD", ProviderIcons.initials("Madara Demo"))
    }

    @Test
    fun resolvedIconUrl_prefersExplicit() {
        assertEquals(
            "https://cdn.example/icon.png",
            ProviderIcons.resolvedIconUrl("https://cdn.example/icon.png", "https://site.test"),
        )
    }

    @Test
    fun resolvedIconUrl_fallsBackToFavicon() {
        val url = ProviderIcons.resolvedIconUrl("", "https://lightnoveldaily.com/")
        assertTrue(url!!.contains("lightnoveldaily.com"))
        assertTrue(url.contains("favicons"))
    }

    @Test
    fun resolvedIconUrl_nullWhenNoHost() {
        assertNull(ProviderIcons.resolvedIconUrl(null, "not-a-url"))
        assertNull(ProviderIcons.faviconUrl(""))
    }
}
