package com.example.quicknovel

import com.lagradost.quicknovel.DownloadTranslateEngine
import com.lagradost.quicknovel.DownloadTranslateLangs
import com.lagradost.quicknovel.DownloadTranslateRequest
import com.lagradost.quicknovel.GeminiProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadTranslateTest {
    @Test
    fun spanish_isFirstOfferedLanguage() {
        assertEquals("es", DownloadTranslateLangs.ALL.first().code)
        assertEquals("Español", DownloadTranslateLangs.ALL.first().displayName)
        assertNotNull(DownloadTranslateLangs.byCode("EN")?.englishName)
        assertEquals("English", DownloadTranslateLangs.byCode("en")?.englishName)
        assertNull(DownloadTranslateLangs.byCode("xx"))
    }

    @Test
    fun offeredList_includesCommonTargets() {
        val codes = DownloadTranslateLangs.ALL.map { it.code }.toSet()
        assertTrue(codes.containsAll(listOf("es", "en", "pt", "fr", "de", "ja", "zh")))
    }

    @Test
    fun cacheSuffix_encodesLanguageAndEngine() {
        val es = DownloadTranslateLangs.byCode("es")!!
        assertEquals(
            "es.gemini",
            DownloadTranslateRequest(es, DownloadTranslateEngine.Gemini).cacheSuffix,
        )
        assertEquals(
            "en.gtx",
            DownloadTranslateRequest(
                DownloadTranslateLangs.byCode("en")!!,
                DownloadTranslateEngine.GoogleOnline,
            ).cacheSuffix,
        )
    }

    @Test
    fun geminiPrompt_usesChosenLanguageName() {
        assertTrue(GeminiProtocol.systemPrompt("Spanish").contains("fluent Spanish"))
        assertTrue(GeminiProtocol.systemPrompt("French").contains("fluent French"))
        assertFalse(GeminiProtocol.systemPrompt("French").contains("fluent Spanish"))
        val body = GeminiProtocol.buildRequest("<p>Hi</p>", "Portuguese")
        assertTrue(body.contains("fluent Portuguese"))
    }
}
