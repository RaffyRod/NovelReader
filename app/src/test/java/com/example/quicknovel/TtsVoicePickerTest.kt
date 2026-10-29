package com.example.quicknovel

import com.lagradost.quicknovel.util.TtsVoicePicker
import com.lagradost.quicknovel.util.VoiceCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class TtsVoicePickerTest {
    private fun voice(name: String, language: String = "es", quality: Int = 400, network: Boolean = false, installed: Boolean = true) =
        VoiceCandidate(name, language, quality, network, installed)

    @Test
    fun best_prefersHigherQuality() {
        val voices = listOf(voice("basic", quality = 300), voice("neural", quality = 500))
        assertEquals("neural", TtsVoicePicker.best(voices, "es"))
    }

    @Test
    fun best_prefersOfflineOnQualityTie() {
        val voices = listOf(voice("online", network = true), voice("offline"))
        assertEquals("offline", TtsVoicePicker.best(voices, "es"))
    }

    @Test
    fun best_ignoresOtherLanguagesAndMissingData() {
        val voices = listOf(
            voice("english", language = "en", quality = 500),
            voice("notInstalled", quality = 500, installed = false),
            voice("spanish", quality = 200),
        )
        assertEquals("spanish", TtsVoicePicker.best(voices, "es"))
    }

    @Test
    fun best_returnsNullWhenNoVoiceFits() {
        assertNull(TtsVoicePicker.best(listOf(voice("english", language = "en")), "es"))
    }

    @Test
    fun preferredLocale_keepsCountryWhenAvailable() {
        val available = listOf(Locale("es", "ES"), Locale("es", "MX"), Locale.US)
        assertEquals(Locale("es", "MX"), TtsVoicePicker.preferredLocale(Locale("es", "MX"), available))
    }

    @Test
    fun preferredLocale_usesSameLanguageWhenCountryDiffers() {
        val available = listOf(Locale("es", "ES"), Locale.US)
        assertEquals(Locale("es", "ES"), TtsVoicePicker.preferredLocale(Locale("es"), available))
    }

    @Test
    fun preferredLocale_fallsBackWhenLanguageIsMissing() {
        assertEquals(Locale.US, TtsVoicePicker.preferredLocale(Locale("es"), listOf(Locale.FRANCE)))
    }
}
