package com.example.quicknovel

import com.lagradost.quicknovel.util.CustomProviders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomProvidersStoreKeyTest {
    @Test
    fun storeKey_isStableForAsciiNames() {
        assertEquals(
            CustomProviders.storeKey("DevilNovels"),
            CustomProviders.storeKey("devilnovels"),
        )
        assertTrue(CustomProviders.storeKey("SkyNovels").matches(Regex("[0-9a-f]+")))
    }

    @Test
    fun storeKey_keepsUnicodeNamesDistinct() {
        val arabic = CustomProviders.storeKey("رواية عربية")
        val chinese = CustomProviders.storeKey("中文小说")
        val russian = CustomProviders.storeKey("Русский")
        assertNotEquals(arabic, chinese)
        assertNotEquals(chinese, russian)
        assertNotEquals(arabic, CustomProviders.storeKey("provider"))
        // Old ASCII sanitizer collapsed all of these to the same bucket.
        assertNotEquals(
            CustomProviders.legacyAsciiStoreKey("رواية عربية"),
            CustomProviders.storeKey("رواية عربية"),
        )
        assertEquals("provider", CustomProviders.legacyAsciiStoreKey(" رواية "))
    }

    @Test
    fun storeKey_differsFromLegacyHash() {
        val name = "SkyNovels"
        assertTrue(CustomProviders.legacyStoreKey(name).matches(Regex("-?\\d+")))
        assertNotEquals(CustomProviders.storeKey(name), CustomProviders.legacyStoreKey(name))
    }
}
