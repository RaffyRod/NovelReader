package com.example.quicknovel

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards ES/EN string parity for user-facing keys so CI catches missing translations early.
 */
class LocaleStringsParityTest {
    private val root = File("src/main/res")

    private fun stringNames(file: File): Set<String> {
        val text = file.readText()
        return Regex("""<string\s+name="([^"]+)"""").findAll(text).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun es_coversCriticalEnglishKeys() {
        val en = stringNames(File(root, "values/strings.xml"))
        val es = stringNames(File(root, "values-es/strings.xml"))
        val critical = listOf(
            "dark_mode_settings",
            "ui_icon_theme_settings",
            "select_all",
            "select_none",
            "search_providers",
            "show_english_providers",
            "allow_special_search_chars",
        )
        val missing = critical.filter { it in en && it !in es }
        assertTrue("ES missing critical keys: $missing", missing.isEmpty())
    }

    @Test
    fun es_parity_within_tolerance() {
        val en = stringNames(File(root, "values/strings.xml"))
        val es = stringNames(File(root, "values-es/strings.xml"))
        val missingInEs = en - es
        // Allow a small intentional English-only set (debug / legacy), but keep it bounded.
        assertTrue(
            "Too many EN keys missing in ES (${missingInEs.size}): ${missingInEs.take(20)}",
            missingInEs.size <= 60,
        )
        val missingInEn = es - en
        assertTrue(
            "ES keys without EN base (${missingInEn.size}): ${missingInEn.take(20)}",
            missingInEn.size <= 20,
        )
    }
}
