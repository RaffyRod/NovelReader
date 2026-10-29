package com.example.quicknovel

import com.lagradost.quicknovel.util.RecentSearches
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentSearchesTest {
    @Test
    fun `the newest search goes first`() {
        assertEquals(listOf("mago", "dragón"), RecentSearches.add(listOf("dragón"), "mago"))
    }

    @Test
    fun `a repeated search moves to the front without duplicates`() {
        assertEquals(listOf("Dragón", "mago"), RecentSearches.add(listOf("mago", "dragón"), " Dragón "))
    }

    @Test
    fun `blank searches are ignored`() {
        assertEquals(listOf("mago"), RecentSearches.add(listOf("mago"), "   "))
    }

    @Test
    fun `the list never grows past the maximum`() {
        val full = (1..RecentSearches.MAX).map { "q$it" }
        val result = RecentSearches.add(full, "nuevo")
        assertEquals(RecentSearches.MAX, result.size)
        assertEquals("nuevo", result.first())
    }
}
