package com.lagradost.quicknovel.util

/** Decides which provider languages are shown: Spanish by default, English only when enabled. */
object ProviderLanguages {
    const val DEFAULT = "es"
    const val ENGLISH = "en"
    const val SHOW_ENGLISH_KEY = "show_english_providers"

    /**
     * Languages whose providers are listed. English is controlled only by [showEnglish],
     * and the result is never empty so the search screen always has providers.
     */
    fun effective(selected: Set<String>, showEnglish: Boolean): Set<String> {
        val others = selected - ENGLISH
        val base = others.ifEmpty { setOf(DEFAULT) }
        return if (showEnglish) base + ENGLISH else base
    }
}
