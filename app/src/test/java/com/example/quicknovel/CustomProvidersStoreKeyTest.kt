package com.example.quicknovel

import com.lagradost.quicknovel.util.CustomProviders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomProvidersStoreKeyTest {
    @Test
    fun storeKey_isStableAndCollisionResistant() {
        assertEquals("devilnovels", CustomProviders.storeKey("DevilNovels"))
        assertEquals("my_novel_source", CustomProviders.storeKey("My Novel Source"))
        assertNotEquals(
            CustomProviders.storeKey("Novel Fire"),
            CustomProviders.storeKey("NovelFull"),
        )
    }

    @Test
    fun storeKey_differsFromLegacyHashForTypicalNames() {
        val name = "SkyNovels"
        // New key is readable; legacy is a signed hash string.
        assertTrue(CustomProviders.storeKey(name).all { it.isLetterOrDigit() || it == '_' || it == '.' || it == '-' })
        assertTrue(CustomProviders.legacyStoreKey(name).matches(Regex("-?\\d+")))
    }
}
