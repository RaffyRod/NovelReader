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
    fun best_prefersPiperDaveForSpanish() {
        val voices = listOf(
            voice("es-ES-neural", quality = 500),
            voice("es_ES-davefx-medium", quality = 400),
            voice("online", quality = 500, network = true),
        )
        assertEquals("es_ES-davefx-medium", TtsVoicePicker.best(voices, "es"))
    }

    @Test
    fun best_prefersLessacForEnglish() {
        val voices = listOf(
            voice("en-us-x-sfg-local", language = "en", quality = 500),
            voice("en_US-lessac-medium", language = "en", quality = 400),
        )
        assertEquals("en_US-lessac-medium", TtsVoicePicker.best(voices, "en"))
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
    fun preferredLocale_usesAvailableLanguageWhenWantedMissing() {
        // Prefer Spanish if present, otherwise the first installed locale (never invent one).
        assertEquals(Locale.US, TtsVoicePicker.preferredLocale(Locale("fr"), listOf(Locale.US)))
        assertEquals(Locale("es", "ES"), TtsVoicePicker.preferredLocale(Locale("fr"), listOf(Locale.US, Locale("es", "ES"))))
    }

    @Test
    fun detectLanguage_spanishText() {
        assertEquals(
            "es",
            TtsVoicePicker.detectLanguage("Soy una don nadie, sin tiempo para fijarme en el brillo del sol.")
        )
    }

    @Test
    fun detectLanguage_englishText() {
        assertEquals(
            "en",
            TtsVoicePicker.detectLanguage("I was a nobody with no time to look at the sun and the sky.")
        )
    }

    @Test
    fun resolveReadingLanguage_usesProviderHintWhenSampleUnclear() {
        assertEquals(
            "es",
            TtsVoicePicker.resolveReadingLanguage(
                sampleText = "SonicMTL",
                contentLanguageHint = "es",
                appLocale = Locale.US,
            ),
        )
    }

    @Test
    fun resolveReadingLanguage_prefersDetectedOverEnglishApp() {
        assertEquals(
            "es",
            TtsVoicePicker.resolveReadingLanguage(
                sampleText = "Finalmente encontré trabajo en la morgue del hospital.",
                contentLanguageHint = "en",
                appLocale = Locale.US,
            ),
        )
    }

    @Test
    fun readingLocale_ignoresSavedEnglishWhenTextIsSpanish() {
        val available = listOf(Locale("es", "ES"), Locale.US)
        val locale = TtsVoicePicker.readingLocale(
            available = available,
            appLocale = Locale.US,
            sampleText = "Finalmente encontré trabajo en la morgue del hospital.",
            savedDisplayName = Locale.US.displayName,
        )
        assertEquals("es", locale.language)
    }

    @Test
    fun readingLocale_usesHintWhenSampleBlankAndAppIsEnglish() {
        val available = listOf(Locale("es", "ES"), Locale.US)
        val locale = TtsVoicePicker.readingLocale(
            available = available,
            appLocale = Locale.US,
            sampleText = "",
            savedDisplayName = Locale.US.displayName,
            contentLanguageHint = "es",
        )
        assertEquals("es", locale.language)
    }

    @Test
    fun effectiveSpeechRate_slowsNeuralEnglish() {
        val sherpa = "com.k2fsa.sherpa.onnx.tts.engine"
        assertEquals(0.62f, TtsVoicePicker.effectiveSpeechRate(1.0f, sherpa, "en"), 0.001f)
        assertEquals(1.0f, TtsVoicePicker.effectiveSpeechRate(1.0f, "com.google.android.tts", "en"), 0.001f)
    }
}
