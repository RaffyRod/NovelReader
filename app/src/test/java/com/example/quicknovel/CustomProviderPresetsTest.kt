package com.example.quicknovel

import com.lagradost.quicknovel.providers.CustomProviderParser
import com.lagradost.quicknovel.providers.CustomProviderPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomProviderPresetsTest {
    @Test
    fun `a bare domain gets https`() {
        assertEquals("https://misitio.com", CustomProviderPresets.normalizeBaseUrl("misitio.com"))
    }

    @Test
    fun `the path and trailing slash are dropped`() {
        assertEquals(
            "https://misitio.com",
            CustomProviderPresets.normalizeBaseUrl("  https://misitio.com/novela/mi-libro/ ")
        )
    }

    @Test
    fun `http and ports are kept`() {
        assertEquals(
            "http://misitio.com:8080",
            CustomProviderPresets.normalizeBaseUrl("http://misitio.com:8080/x")
        )
    }

    @Test
    fun `text that is not an address is rejected`() {
        assertNull(CustomProviderPresets.normalizeBaseUrl("mi sitio"))
        assertNull(CustomProviderPresets.normalizeBaseUrl(""))
        assertNull(CustomProviderPresets.normalizeBaseUrl("hola"))
    }

    @Test
    fun `every candidate is a valid provider`() {
        val candidates = CustomProviderPresets.candidates("Mi sitio", "https://misitio.com", "es")
        assertTrue(candidates.isNotEmpty())
        candidates.forEach {
            assertNull(CustomProviderParser.validate(it))
            assertEquals("es", it.lang)
        }
    }
}
