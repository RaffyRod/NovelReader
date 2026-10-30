package com.lagradost.quicknovel.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.preference.PreferenceManager
import com.lagradost.quicknovel.R
import java.util.Locale

/** What the voice picker needs to know about an installed text to speech voice. */
data class VoiceCandidate(
    val name: String,
    val language: String,
    val quality: Int,
    val needsNetwork: Boolean,
    val installed: Boolean,
)

/** Chooses the language and voice used to read aloud when the reader has not picked one. */
object TtsVoicePicker {
    /** Preferred neural engines (Piper via Sherpa / Hayai). First installed wins. */
    private val PREFERRED_ENGINE_HINTS = listOf(
        "sherpa",
        "k2fsa",
        "hayai",
        "piper",
    )

    /**
     * Preferred voice name fragments for natural offline reading.
     * Spanish: Piper Dave (es_ES-davefx-medium). English: Piper Lessac / Amy.
     */
    private val PREFERRED_VOICE_HINTS_BY_LANG = mapOf(
        "es" to listOf("davefx", "dave", "miro", "sharvard", "piper", "sherpa", "vits"),
        "en" to listOf("lessac", "amy", "ryan", "piper", "sherpa", "vits"),
    )

    private val SPANISH_MARKERS = listOf(
        " el ", " la ", " de ", " que ", " y ", " en ", " los ", " las ", " del ",
        " una ", " por ", " con ", " para ", " como ", " más ", " pero ", " sus ",
    )
    private val ENGLISH_MARKERS = listOf(
        " the ", " and ", " of ", " to ", " in ", " that ", " was ", " for ",
        " with ", " he ", " she ", " they ", " from ", " this ", " have ",
    )

    /** App UI language (defaults to Spanish), not the device locale. */
    fun appLocale(context: Context): Locale {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val code = prefs.getString(context.getString(R.string.locale_key), "es") ?: "es"
        return Locale.forLanguageTag(code.replace('_', '-'))
    }

    /**
     * Guesses "es" or "en" from a text sample. Returns null when unclear.
     * Spanish punctuation / ñ count strongly toward Spanish.
     */
    fun detectLanguage(sample: String): String? {
        if (sample.isBlank()) return null
        val padded = " ${sample.lowercase(Locale.ROOT)} "
        var es = 0
        var en = 0
        if (sample.any { it in "áéíóúñü¿¡ÁÉÍÓÚÑÜ" }) es += 4
        for (w in SPANISH_MARKERS) if (padded.contains(w)) es++
        for (w in ENGLISH_MARKERS) if (padded.contains(w)) en++
        return when {
            es >= en + 2 -> "es"
            en >= es + 2 -> "en"
            else -> null
        }
    }

    /**
     * Locale to use for reading: saved choice if it still matches the needed language,
     * else detected text language, else app language, else device fallback.
     */
    fun readingLocale(
        available: Collection<Locale>,
        appLocale: Locale,
        sampleText: String?,
        savedDisplayName: String?,
    ): Locale {
        val detected = sampleText?.let { detectLanguage(it) }
        val wantedLanguage = detected
            ?: appLocale.language.takeIf { it.isNotBlank() }
            ?: "es"
        val saved = available.firstOrNull { it.displayName == savedDisplayName }
        if (saved != null && saved.language == wantedLanguage) {
            return saved
        }
        val wanted = Locale(wantedLanguage, if (wantedLanguage == "es") "ES" else "US")
        return preferredLocale(wanted, available, fallback = preferredLocale(appLocale, available))
    }

    /** The first available locale in [wanted]'s language, or [fallback] when there is none. */
    fun preferredLocale(wanted: Locale, available: Collection<Locale>, fallback: Locale = Locale("es", "ES")): Locale {
        val sameLanguage = available.filter { it.language == wanted.language }
        return sameLanguage.firstOrNull { it.country.equals(wanted.country, ignoreCase = true) }
            ?: sameLanguage.firstOrNull()
            ?: available.firstOrNull { it.language == "es" }
            ?: available.firstOrNull()
            ?: fallback
    }

    /**
     * Best installed voice for [language]: neural Piper/Sherpa names first,
     * then highest quality, offline preferred on a tie.
     */
    fun best(candidates: List<VoiceCandidate>, language: String): String? {
        val hints = PREFERRED_VOICE_HINTS_BY_LANG[language].orEmpty()
        return candidates
            .filter { it.language == language && it.installed }
            .sortedWith(
                compareByDescending<VoiceCandidate> { neuralScore(it.name, hints) }
                    .thenByDescending { it.quality }
                    .thenBy { it.needsNetwork }
                    .thenBy { it.name }
            )
            .firstOrNull()
            ?.name
    }

    /**
     * Package name of a preferred neural TTS engine for [language], or any neural engine,
     * or null when none is installed.
     */
    fun preferredEnginePackage(context: Context, language: String? = null): String? {
        val pm = context.packageManager
        val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
        val services = pm.queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val packages = services.mapNotNull { it.serviceInfo?.packageName }.distinct()
        val neural = packages.filter { pkg ->
            PREFERRED_ENGINE_HINTS.any { hint -> pkg.contains(hint, ignoreCase = true) }
        }
        if (neural.isEmpty()) return null
        val lang = language?.lowercase(Locale.ROOT)
        if (lang == "es") {
            neural.firstOrNull { pkg ->
                listOf("davefx", "dave", "_es", "-es-", "spa", "spanish").any {
                    pkg.contains(it, ignoreCase = true)
                }
            }?.let { return it }
        }
        if (lang == "en") {
            neural.firstOrNull { pkg ->
                listOf("lessac", "amy", "ryan", "_en", "-en-", "eng", "english").any {
                    pkg.contains(it, ignoreCase = true)
                }
            }?.let { return it }
        }
        return neural.firstOrNull()
    }

    /** True when a Sherpa / Hayai / Piper TTS engine package is installed. */
    fun hasNeuralEngine(context: Context): Boolean = preferredEnginePackage(context) != null

    fun isNeuralPackage(enginePackage: String?): Boolean {
        if (enginePackage.isNullOrBlank()) return false
        return PREFERRED_ENGINE_HINTS.any { hint -> enginePackage.contains(hint, ignoreCase = true) }
    }

    /**
     * Piper/Sherpa at Android rate 1.0 sounds much faster than Google TTS.
     * Scale the user slider so "1.0" is comfortable for neural voices.
     */
    fun effectiveSpeechRate(userSpeed: Float, enginePackage: String?, language: String?): Float {
        val factor = when {
            !isNeuralPackage(enginePackage) -> 1.0f
            language.equals("en", ignoreCase = true) -> 0.62f
            language.equals("es", ignoreCase = true) -> 0.72f
            else -> 0.68f
        }
        return (userSpeed * factor).coerceIn(0.1f, 2.5f)
    }

    /** Pause between sentences for neural engines (ms). */
    fun neuralSentencePauseMs(language: String?): Long =
        if (language.equals("en", ignoreCase = true)) 380L else 280L

    /** Google TTS package used when the neural engine cannot speak [language]. */
    const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"

    /** System / Google engine package if installed, otherwise null (platform default). */
    fun fallbackEnginePackage(context: Context): String? {
        val pm = context.packageManager
        val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
        val packages = pm.queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { it.serviceInfo?.packageName }
            .distinct()
        return packages.firstOrNull { it == GOOGLE_TTS_PACKAGE }
            ?: packages.firstOrNull { pkg ->
                PREFERRED_ENGINE_HINTS.none { hint -> pkg.contains(hint, ignoreCase = true) }
            }
    }

    private fun neuralScore(name: String, hints: List<String>): Int {
        val lower = name.lowercase(Locale.ROOT)
        val index = hints.indexOfFirst { lower.contains(it) }
        return if (index < 0) 0 else (hints.size - index)
    }
}
