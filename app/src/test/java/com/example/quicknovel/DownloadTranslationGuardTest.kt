package com.example.quicknovel

import com.lagradost.quicknovel.DownloadTranslationGuard
import com.lagradost.quicknovel.LoadedChapter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadTranslationGuardTest {
    @Test
    fun rejectsIdenticalSourceAndTarget() {
        val chapter = LoadedChapter("Title", "<p>Hello world this is a longer sample chapter text.</p>")
        assertFalse(DownloadTranslationGuard.looksTranslated(chapter, chapter))
    }

    @Test
    fun acceptsClearlyTranslatedSpanish() {
        val source = LoadedChapter(
            "Chapter 1",
            "<p>The hero walked into the dark forest and found an ancient sword of power.</p>",
        )
        val translated = LoadedChapter(
            "Capítulo 1",
            "<p>El héroe entró en el bosque oscuro y encontró una antigua espada de poder.</p>",
        )
        assertTrue(DownloadTranslationGuard.looksTranslated(source, translated))
    }

    @Test
    fun rejectsTinyCosmeticChange() {
        val source = LoadedChapter(
            "Title",
            "<p>The hero walked into the dark forest and found an ancient sword of power today.</p>",
        )
        val almost = LoadedChapter(
            "Title",
            "<p>The hero walked into the dark forest and found an ancient sword of power.</p>",
        )
        assertFalse(DownloadTranslationGuard.looksTranslated(source, almost))
    }

    @Test
    fun alreadyInLanguage_detectsSpanishAndSkipsEnglish() {
        val spanish = LoadedChapter(
            "Capítulo 1",
            "<p>El héroe entró en el bosque oscuro y encontró una antigua espada de poder.</p>",
        )
        val english = LoadedChapter(
            "Chapter 1",
            "<p>The hero walked into the dark forest and found an ancient sword of power.</p>",
        )
        assertTrue(DownloadTranslationGuard.alreadyInLanguage(spanish, "es"))
        assertFalse(DownloadTranslationGuard.alreadyInLanguage(english, "es"))
        assertTrue(DownloadTranslationGuard.alreadyInLanguage(english, "en"))
    }
}
