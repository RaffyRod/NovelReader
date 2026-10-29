package com.lagradost.quicknovel

/** Helpers to read, format and label chapter numbers (always 1 based, as shown to the user). */
object ChapterSpec {
    private val separators = Regex("[,;\\s]+")
    private val rangeDash = Regex("\\s*[-–—]\\s*")

    /**
     * Parses text such as "1-10, 15, 20-22" into a sorted set of chapter numbers.
     * Returns null when any token is malformed or outside `1..total`.
     */
    fun parse(text: String, total: Int): Set<Int>? {
        val tokens = text.trim().replace(rangeDash, "-").split(separators).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null

        val result = sortedSetOf<Int>()
        for (token in tokens) {
            val parts = token.split('-')
            val numbers = parts.map { it.toIntOrNull() }
            if (numbers.any { it == null } || parts.size > 2) return null
            val from = numbers.first()!!
            val to = numbers.last()!!
            val start = minOf(from, to)
            val end = maxOf(from, to)
            if (start < 1 || end > total) return null
            result.addAll(start..end)
        }
        return result
    }

    /** Formats chapter numbers compactly, for example [1,2,3,9] becomes "1-3, 9". */
    fun format(numbers: Collection<Int>, separator: String = ", "): String {
        val sorted = numbers.toSortedSet().toList()
        if (sorted.isEmpty()) return ""

        val parts = ArrayList<String>()
        var start = sorted.first()
        var previous = start
        for (number in sorted.drop(1) + Int.MIN_VALUE) {
            if (number == previous + 1) {
                previous = number
                continue
            }
            parts.add(if (start == previous) "$start" else "$start-$previous")
            start = number
            previous = number
        }
        return parts.joinToString(separator)
    }

    /** Prefixes the title with its chapter number unless the title already starts with it. */
    fun numberedTitle(number: Int, title: String): String {
        val clean = title.trim()
        val alreadyNumbered = Regex("^(chapter|cap[ií]tulo|cap\\.?)?\\s*#?$number\\b", RegexOption.IGNORE_CASE)
        return when {
            clean.isEmpty() -> "$number"
            alreadyNumbered.containsMatchIn(clean) -> clean
            else -> "$number. $clean"
        }
    }

    /** Splits the chapter numbers into groups of [perFile], or a single group when it is not positive. */
    fun group(numbers: List<Int>, perFile: Int): List<List<Int>> {
        if (numbers.isEmpty()) return emptyList()
        return if (perFile <= 0) listOf(numbers) else numbers.chunked(perFile)
    }
}
