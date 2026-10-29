package com.lagradost.quicknovel.util

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
    /** The first available locale in [wanted]'s language, or [fallback] when there is none. */
    fun preferredLocale(wanted: Locale, available: Collection<Locale>, fallback: Locale = Locale.US): Locale {
        val sameLanguage = available.filter { it.language == wanted.language }
        return sameLanguage.firstOrNull { it.country == wanted.country }
            ?: sameLanguage.firstOrNull()
            ?: fallback
    }

    /** The name of the best installed voice for [language]: highest quality, offline ones first on a tie. */
    fun best(candidates: List<VoiceCandidate>, language: String): String? =
        candidates
            .filter { it.language == language && it.installed }
            .sortedWith(compareByDescending<VoiceCandidate> { it.quality }.thenBy { it.needsNetwork }.thenBy { it.name })
            .firstOrNull()
            ?.name
}
