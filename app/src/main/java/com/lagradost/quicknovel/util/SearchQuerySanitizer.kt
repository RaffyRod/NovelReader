package com.lagradost.quicknovel.util

/**
 * Filters search-box input. By default only letters, digits, spaces, hyphen, apostrophe,
 * `.` `:` and `/` are kept so symbols like @ # $ % do not reach provider queries while host
 * / URL filters such as `wattpad.com` still match the provider grid.
 */
object SearchQuerySanitizer {
    /** Matches characters that are blocked when special chars are disabled. */
    private val BLOCKED_WHEN_STRICT = Regex("""[^\p{L}\p{N}\s\-'.:/]""")

    /**
     * @param input raw text from the search field
     * @param allowSpecialChars when true, [input] is returned unchanged
     */
    fun filter(input: String, allowSpecialChars: Boolean): String {
        if (allowSpecialChars) return input
        return input.replace(BLOCKED_WHEN_STRICT, "")
    }

    /** True when [input] contains at least one character that would be stripped. */
    fun containsSpecialChars(input: String): Boolean =
        BLOCKED_WHEN_STRICT.containsMatchIn(input)
}
