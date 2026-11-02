package com.example.quicknovel

import com.lagradost.quicknovel.TranslationHistoryEntry
import com.lagradost.quicknovel.TranslationHistoryLogic
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationHistoryLogicTest {
    @Test
    fun add_newestFirst_dedupesByUri() {
        val a = TranslationHistoryEntry(novelName = "A", fileName = "a.epub", uri = "content://a", time = 1)
        val b = TranslationHistoryEntry(novelName = "B", fileName = "b.epub", uri = "content://b", time = 2)
        val a2 = a.copy(fileName = "a-renamed.epub", time = 3)
        val list = TranslationHistoryLogic.add(TranslationHistoryLogic.add(listOf(a), b), a2)
        assertEquals(listOf("content://a", "content://b"), list.map { it.uri })
        assertEquals("a-renamed.epub", list.first().fileName)
    }

    @Test
    fun remove_byUri() {
        val a = TranslationHistoryEntry(uri = "content://a")
        val b = TranslationHistoryEntry(uri = "content://b")
        assertEquals(listOf(b), TranslationHistoryLogic.remove(listOf(a, b), "content://a"))
    }

    @Test
    fun keepExisting_dropsMissing() {
        val a = TranslationHistoryEntry(uri = "content://a", folderUri = "content://folder")
        val b = TranslationHistoryEntry(uri = "content://b")
        assertEquals(
            listOf(a),
            TranslationHistoryLogic.keepExisting(listOf(a, b)) { it.uri == "content://a" },
        )
    }
}
