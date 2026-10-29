package com.example.quicknovel

import com.lagradost.quicknovel.util.ProviderLanguages
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderLanguagesTest {
    @Test
    fun `only Spanish is listed by default`() {
        assertEquals(setOf("es"), ProviderLanguages.effective(setOf("es"), showEnglish = false))
    }

    @Test
    fun `English is added when the option is enabled`() {
        assertEquals(setOf("es", "en"), ProviderLanguages.effective(setOf("es"), showEnglish = true))
    }

    @Test
    fun `English in the saved list is ignored while the option is off`() {
        assertEquals(setOf("es", "fr"), ProviderLanguages.effective(setOf("es", "en", "fr"), showEnglish = false))
    }

    @Test
    fun `an empty selection falls back to Spanish`() {
        assertEquals(setOf("es"), ProviderLanguages.effective(emptySet(), showEnglish = false))
        assertEquals(setOf("es", "en"), ProviderLanguages.effective(setOf("en"), showEnglish = true))
    }
}
