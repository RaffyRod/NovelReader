package com.lagradost.quicknovel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChapterSpecTest {
    @Test
    fun parse_range_returnsEveryChapter() {
        assertEquals((1..10).toSet(), ChapterSpec.parse("1-10", 50))
    }

    @Test
    fun parse_nonContiguousChapters_returnsOnlyThoseChapters() {
        assertEquals(setOf(1, 9), ChapterSpec.parse("1, 9", 50))
    }

    @Test
    fun parse_mixedRangesAndSingles_isSortedAndDeduplicated() {
        assertEquals(setOf(2, 3, 4, 8, 15), ChapterSpec.parse("15; 2 - 4, 3, 8", 50))
    }

    @Test
    fun parse_reversedRange_isNormalized() {
        assertEquals(setOf(3, 4, 5), ChapterSpec.parse("5-3", 50))
    }

    @Test
    fun parse_invalidInput_returnsNull() {
        assertNull(ChapterSpec.parse("", 50))
        assertNull(ChapterSpec.parse("abc", 50))
        assertNull(ChapterSpec.parse("0-3", 50))
        assertNull(ChapterSpec.parse("45-51", 50))
        assertNull(ChapterSpec.parse("1-2-3", 50))
        assertNull(ChapterSpec.parse("-5", 50))
    }

    @Test
    fun format_compressesConsecutiveChapters() {
        assertEquals("1-3, 9", ChapterSpec.format(listOf(1, 2, 3, 9)))
        assertEquals("1, 9", ChapterSpec.format(setOf(9, 1)))
        assertEquals("5", ChapterSpec.format(listOf(5)))
        assertEquals("1-10", ChapterSpec.format((1..10).toList()))
        assertEquals("", ChapterSpec.format(emptyList()))
    }

    @Test
    fun numberedTitle_addsOnlyMissingNumber() {
        assertEquals("9. The Bitter Truth", ChapterSpec.numberedTitle(9, "The Bitter Truth"))
        assertEquals("3. The Bitter Truth", ChapterSpec.numberedTitle(3, "3. The Bitter Truth"))
        assertEquals("Chapter 12: Storm", ChapterSpec.numberedTitle(12, "Chapter 12: Storm"))
        assertEquals("1. 12 Angry Men", ChapterSpec.numberedTitle(1, "12 Angry Men"))
    }

    @Test
    fun listTitle_localizesGenericChapterNames() {
        assertEquals("Capítulo 2", ChapterSpec.listTitle(2, "Chapter 2", "Capítulo"))
        assertEquals("Capítulo 3", ChapterSpec.listTitle(3, "Capítulo 3", "Capítulo"))
        assertEquals(
            "Capítulo 4. La tormenta",
            ChapterSpec.listTitle(4, "Chapter 4: La tormenta", "Capítulo"),
        )
        assertEquals("5. The Bitter Truth", ChapterSpec.listTitle(5, "The Bitter Truth", "Capítulo"))
    }

    @Test
    fun listTitle_preservesDecimalChapterNumbers() {
        // List position must NOT overwrite 2.5 / 3.3 from the provider title.
        assertEquals("Capítulo 2.5", ChapterSpec.listTitle(7, "Chapter 2.5", "Capítulo"))
        assertEquals("Capítulo 3.3", ChapterSpec.listTitle(9, "Capítulo 3.3", "Capítulo"))
        assertEquals(
            "Capítulo 2.5. Extra scene",
            ChapterSpec.listTitle(7, "Chapter 2.5: Extra scene", "Capítulo"),
        )
        assertEquals("Capítulo 1.5", ChapterSpec.listTitle(3, "1.5", "Capítulo"))
        assertEquals(
            "Capítulo 4.25. Side story",
            ChapterSpec.listTitle(12, "4.25 Side story", "Capítulo"),
        )
    }

    @Test
    fun group_splitsByChaptersPerFile() {
        assertEquals(listOf(listOf(1, 2), listOf(3, 9)), ChapterSpec.group(listOf(1, 2, 3, 9), 2))
        assertEquals(listOf(listOf(1, 2, 3)), ChapterSpec.group(listOf(1, 2, 3), 0))
    }
}
