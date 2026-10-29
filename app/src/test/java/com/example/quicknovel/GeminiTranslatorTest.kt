package com.example.quicknovel

import com.fasterxml.jackson.databind.ObjectMapper
import com.lagradost.quicknovel.GeminiHttpResponse
import com.lagradost.quicknovel.GeminiProtocol
import com.lagradost.quicknovel.GeminiTransport
import com.lagradost.quicknovel.GeminiTranslator
import com.lagradost.quicknovel.LoadedChapter
import com.lagradost.quicknovel.TranslationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GeminiTranslatorTest {
    private val mapper = ObjectMapper()

    private fun reply(text: String) = GeminiHttpResponse(
        200,
        mapper.writeValueAsString(
            mapOf("candidates" to listOf(mapOf("content" to mapOf("parts" to listOf(mapOf("text" to text))))))
        )
    )

    private fun requestText(body: String): String =
        mapper.readTree(body).path("contents").path(0).path("parts").path(0).path("text").asText()

    private fun translator(
        transport: GeminiTransport,
        sleeps: MutableList<Long> = mutableListOf(),
        minIntervalMs: Long = 0,
        maxRetries: Int = 2,
        now: () -> Long = { 0L },
    ) = GeminiTranslator("key", "model", transport, minIntervalMs, maxRetries, { sleeps.add(it) }, now)

    @Test
    fun `code fences around the answer are removed`() {
        assertEquals("<p>Hola</p>", GeminiProtocol.stripCodeFence("```html\n<p>Hola</p>\n```"))
        assertEquals("<p>Hola</p>", GeminiProtocol.stripCodeFence("  <p>Hola</p> "))
    }

    @Test
    fun `long chapters are split between paragraphs without losing any`() {
        val paragraphs = (1..10).map { "<p>${"x".repeat(100)} $it</p>" }
        val chunks = GeminiProtocol.splitHtml(paragraphs.joinToString(""), maxChars = 350)

        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 350 })
        val joined = chunks.joinToString("")
        (1..10).forEach { assertTrue(joined.contains(" $it</p>")) }
        assertTrue(joined.indexOf(" 1</p>") < joined.indexOf(" 10</p>"))
    }

    @Test
    fun `request carries the system prompt and the text to translate`() {
        val root = mapper.readTree(GeminiProtocol.buildRequest("<p>Hello</p>"))

        assertEquals("<p>Hello</p>", requestText(root.toString()))
        assertEquals(
            GeminiProtocol.SYSTEM_PROMPT,
            root.path("systemInstruction").path("parts").path(0).path("text").asText()
        )
    }

    @Test
    fun `blocked answers are reported with their reason`() {
        val reply = GeminiProtocol.parseReply("""{"promptFeedback":{"blockReason":"SAFETY"}}""")

        assertEquals(GeminiProtocol.Reply.Blocked("SAFETY"), reply)
    }

    @Test
    fun `retry delay is read from a quota error`() {
        val body = """{"error":{"details":[{"retryDelay":"34s"}]}}"""

        assertEquals(34_000L, GeminiProtocol.retryDelayMs(body))
        assertNull(GeminiProtocol.retryDelayMs("{}"))
    }

    @Test
    fun `chapter title and text are translated and the title marker is removed`() = runBlocking {
        val transport = GeminiTransport { _, _, body ->
            reply("```html\n" + requestText(body).replace("Hello", "Hola").replace("Title", "Titulo") + "\n```")
        }

        val result = translator(transport).translateChapter(
            LoadedChapter("Title", "<p>Hello world</p><p>Hello again</p>")
        )

        assertEquals("Titulo", result.title)
        assertEquals("<p>Hola world</p>\n<p>Hola again</p>", result.html)
    }

    @Test
    fun `original title is kept when the answer drops the title marker`() = runBlocking {
        val transport = GeminiTransport { _, _, _ -> reply("<p>Hola</p>") }

        val result = translator(transport).translateChapter(LoadedChapter("Title", "<p>Hello</p>"))

        assertEquals("Title", result.title)
        assertEquals("<p>Hola</p>", result.html)
    }

    @Test
    fun `a refused text keeps the original so the chapter stays complete`() = runBlocking {
        val transport = GeminiTransport { _, _, _ ->
            GeminiHttpResponse(200, """{"promptFeedback":{"blockReason":"SAFETY"}}""")
        }

        val result = translator(transport).translateChunk("<p>Hello</p>")

        assertEquals("<p>Hello</p>", result)
    }

    @Test
    fun `quota errors are retried after the delay the server asked for`() = runBlocking {
        var calls = 0
        val sleeps = mutableListOf<Long>()
        val transport = GeminiTransport { _, _, _ ->
            calls++
            if (calls == 1) {
                GeminiHttpResponse(429, """{"error":{"details":[{"retryDelay":"2s"}]}}""")
            } else {
                reply("<p>Hola</p>")
            }
        }

        val result = translator(transport, sleeps).translateChunk("<p>Hello</p>")

        assertEquals("<p>Hola</p>", result)
        assertEquals(2, calls)
        assertEquals(listOf(2000L), sleeps)
    }

    @Test
    fun `quota errors that never clear end with a readable message`() = runBlocking {
        val transport = GeminiTransport { _, _, _ ->
            GeminiHttpResponse(429, """{"error":{"message":"Quota exceeded"}}""")
        }

        try {
            translator(transport, maxRetries = 1).translateChunk("<p>Hello</p>")
            fail("Expected a TranslationException")
        } catch (e: TranslationException) {
            assertTrue(e.message!!.contains("429"))
            assertTrue(e.message!!.contains("Quota exceeded"))
        }
    }

    @Test
    fun `an invalid key fails at once without retrying`() = runBlocking {
        var calls = 0
        val transport = GeminiTransport { _, _, _ ->
            calls++
            GeminiHttpResponse(400, """{"error":{"message":"API key not valid"}}""")
        }

        try {
            translator(transport).translateChunk("<p>Hello</p>")
            fail("Expected a TranslationException")
        } catch (e: TranslationException) {
            assertTrue(e.message!!.contains("API key not valid"))
        }
        assertEquals(1, calls)
    }

    @Test
    fun `requests are spaced to respect the free rate limit`() = runBlocking {
        var clock = 1_000L
        val sleeps = mutableListOf<Long>()
        val transport = GeminiTransport { _, _, _ -> reply("<p>Hola</p>") }
        val translator = GeminiTranslator("key", "model", transport, 4500L, 2, {
            sleeps.add(it)
            clock += it
        }, { clock })

        translator.translateChunk("<p>1</p>")
        clock += 500
        translator.translateChunk("<p>2</p>")

        assertEquals(listOf(4000L), sleeps.takeLast(1))
    }

    @Test
    fun `check returns null when the key works and the message when it does not`() = runBlocking {
        val good = translator(GeminiTransport { _, _, _ -> reply("<p>Hola</p>") })
        val bad = translator(GeminiTransport { _, _, _ -> GeminiHttpResponse(403, "{}") })

        assertNull(good.check())
        assertTrue(bad.check()!!.contains("403"))
    }
}
