package com.example.quicknovel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Documents the client-side relevance filter used when a provider API returns popular
 * noise instead of real matches (SkyNovels `q=` quirk).
 */
class SearchQueryRelevanceTest {

    private fun matches(title: String, query: String): Boolean =
        title.contains(query.trim(), ignoreCase = true)

    @Test
    fun darkQuery_rejectsUnrelatedSkyNovelsTitles() {
        val junk = listOf(
            "Sellando el Tiempo",
            "Super Gen",
            "Inocencia Destrozada: Transmigrada a Novela como Historia Extra",
            "A Will Eternal",
        )
        junk.forEach { title ->
            assertFalse("$title must not match dark", matches(title, "dark"))
        }
    }

    @Test
    fun darkQuery_keepsTitlesThatContainDark() {
        assertTrue(matches("The Dark King", "dark"))
        assertTrue(matches("DARK LORD RETURNS", "dark"))
    }
}
