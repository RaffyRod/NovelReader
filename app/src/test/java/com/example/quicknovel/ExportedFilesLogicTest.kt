package com.example.quicknovel

import com.lagradost.quicknovel.ExportedFile
import com.lagradost.quicknovel.ExportedFilesLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportedFilesLogicTest {
    private fun file(uri: String, mime: String = "application/epub+zip") =
        ExportedFile(name = "$uri.file", uri = uri, mime = mime, time = 1L)

    @Test
    fun `newest file goes first`() {
        val result = ExportedFilesLogic.add(listOf(file("a")), file("b"))

        assertEquals(listOf("b", "a"), result.map { it.uri })
    }

    @Test
    fun `saving the same file again moves it to the top without duplicating it`() {
        val result = ExportedFilesLogic.add(listOf(file("a"), file("b")), file("b"))

        assertEquals(listOf("b", "a"), result.map { it.uri })
    }

    @Test
    fun `list never grows past the limit and drops the oldest`() {
        val current = (1..5).map { file("f$it") }

        val result = ExportedFilesLogic.add(current, file("new"), limit = 3)

        assertEquals(listOf("new", "f1", "f2"), result.map { it.uri })
    }

    @Test
    fun `remove forgets only the given file`() {
        val result = ExportedFilesLogic.remove(listOf(file("a"), file("b")), "a")

        assertEquals(listOf("b"), result.map { it.uri })
    }

    @Test
    fun `pdf files are told apart from epub files`() {
        assertTrue(file("a", "application/pdf").isPdf)
        assertFalse(file("a").isPdf)
    }
}
