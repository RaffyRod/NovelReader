package com.example.quicknovel

import com.lagradost.quicknovel.util.SearchQuerySanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQuerySanitizerTest {
    @Test
    fun `strict mode strips symbols but keeps letters digits spaces hyphen and apostrophe`() {
        assertEquals(
            "Mother of Learning-vol 1's",
            SearchQuerySanitizer.filter("Mother of Learning-vol 1's!!!@@@", allowSpecialChars = false),
        )
    }

    @Test
    fun `strict mode keeps accented letters`() {
        assertEquals(
            "Corazón de dragón",
            SearchQuerySanitizer.filter("Corazón de dragón#$%", allowSpecialChars = false),
        )
    }

    @Test
    fun `strict mode removes punctuation and emoji but keeps host dots colons and slashes`() {
        assertEquals(
            "martial:./",
            SearchQuerySanitizer.filter("martial@#\$%^&*()[]{}|;:\"<>?,./~`⭐", allowSpecialChars = false),
        )
    }

    @Test
    fun `strict mode keeps hostnames for provider grid filter`() {
        assertEquals(
            "wattpad.com",
            SearchQuerySanitizer.filter("wattpad.com", allowSpecialChars = false),
        )
        assertEquals(
            "https://www.skynovels.net",
            SearchQuerySanitizer.filter("https://www.skynovels.net", allowSpecialChars = false),
        )
    }

    @Test
    fun `allow mode keeps every character`() {
        val raw = "a@b#c \$% ⭐!"
        assertEquals(raw, SearchQuerySanitizer.filter(raw, allowSpecialChars = true))
    }

    @Test
    fun `containsSpecialChars detects blocked symbols`() {
        assertTrue(SearchQuerySanitizer.containsSpecialChars("hola@mundo"))
        assertFalse(SearchQuerySanitizer.containsSpecialChars("hola mundo-1"))
        assertFalse(SearchQuerySanitizer.containsSpecialChars("señor's"))
    }

    @Test
    fun `blank and plain text pass through in strict mode`() {
        assertEquals("", SearchQuerySanitizer.filter("", allowSpecialChars = false))
        assertEquals("  abc  ", SearchQuerySanitizer.filter("  abc  ", allowSpecialChars = false))
    }
}
