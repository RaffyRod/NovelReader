package com.lagradost.quicknovel.plugins

import com.lagradost.quicknovel.util.ProviderLanguages

/** Language helpers for LNReader manifest labels (full names, not ISO codes). */
object PluginLangs {
    val PRIORITY_LABELS = listOf("English", "Español")

    /** Strip bidi / isolate marks so Arabic (etc.) stay left-aligned in LTR UI lists. */
    fun cleanLabel(label: String): String =
        label.trim().replace(BIDI_MARKS, "")

    /**
     * Latin UI label for catalog rows. RTL scripts (Arabic, …) are shown in English so every
     * card stays left-aligned and matches the rest of the list.
     */
    fun listLabel(label: String): String {
        val clean = cleanLabel(label)
        return when (codeFor(clean)) {
            "ar" -> "Arabic"
            "he" -> "Hebrew"
            "fa" -> "Persian"
            "ur" -> "Urdu"
            else -> clean
        }
    }

    fun codeFor(label: String): String {
        val l = cleanLabel(label).lowercase()
        return when {
            l.startsWith("english") || l == "en" -> ProviderLanguages.ENGLISH
            l.startsWith("español") || l.startsWith("espanol") || l.startsWith("spanish") || l == "es" -> "es"
            l.startsWith("français") || l.startsWith("francais") || l.startsWith("french") || l == "fr" -> "fr"
            l.startsWith("português") || l.startsWith("portugues") || l.startsWith("portuguese") || l == "pt" -> "pt"
            l.startsWith("русский") || l.startsWith("russian") || l == "ru" -> "ru"
            l.startsWith("türkçe") || l.startsWith("turkce") || l.startsWith("turkish") || l == "tr" -> "tr"
            l.startsWith("العرب") || l.startsWith("arabic") || l == "ar" -> "ar"
            l.startsWith("עבר") || l.startsWith("hebrew") || l == "he" -> "he"
            l.startsWith("فارسی") || l.startsWith("persian") || l.startsWith("farsi") || l == "fa" -> "fa"
            l.startsWith("اردو") || l.startsWith("urdu") || l == "ur" -> "ur"
            l.startsWith("bahasa") || l.startsWith("indonesian") || l == "id" -> "id"
            l.startsWith("中文") || l.startsWith("chinese") || l == "zh" -> "zh"
            l.startsWith("日本語") || l.startsWith("japanese") || l == "ja" -> "ja"
            l.startsWith("한국어") || l.startsWith("조선") || l.startsWith("korean") || l == "ko" -> "ko"
            l.startsWith("tiếng việt") || l.startsWith("vietnamese") || l == "vi" -> "vi"
            l.startsWith("ไทย") || l.startsWith("thai") || l == "th" -> "th"
            l.startsWith("україн") || l.startsWith("ukrainian") || l == "uk" -> "uk"
            l.startsWith("polski") || l.startsWith("polish") || l == "pl" -> "pl"
            l.startsWith("বাংলা") || l.startsWith("bengali") || l == "bn" -> "bn"
            else -> ProviderLanguages.DEFAULT
        }
    }

    /**
     * Orders language labels with English and Español first, then by descending count.
     */
    fun ordered(counts: Map<String, Int>): List<Pair<String, Int>> {
        val priority = PRIORITY_LABELS.filter { counts.containsKey(it) }
            .map { it to (counts[it] ?: 0) }
        val rest = counts.entries
            .filter { it.key !in PRIORITY_LABELS }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key to it.value }
        return priority + rest
    }

    private val BIDI_MARKS = Regex("[\\u200e\\u200f\\u202a-\\u202e\\u2066-\\u2069]")
}
