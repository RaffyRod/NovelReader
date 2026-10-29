package com.lagradost.quicknovel

import com.fasterxml.jackson.databind.ObjectMapper
import com.lagradost.quicknovel.MainActivity.Companion.app
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities
import java.io.IOException

/** A translation failure with a message that can be shown to the user. */
class TranslationException(message: String) : Exception(message)

/** Raw answer of the Gemini endpoint. [code] is 0 when the request never reached the server. */
data class GeminiHttpResponse(val code: Int, val body: String)

/** Sends one generateContent request, replaceable in tests. */
fun interface GeminiTransport {
    suspend fun post(model: String, apiKey: String, body: String): GeminiHttpResponse
}

/** Request and response handling for the Gemini API, free of any Android dependency. */
object GeminiProtocol {
    const val DEFAULT_MODEL = "gemini-2.5-flash"
    val MODELS = listOf("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.0-flash")
    const val MAX_CHUNK_CHARS = 6000
    const val TITLE_CLASS = "qn-title"
    const val KEYS_URL = "https://aistudio.google.com/apikey"

    private val mapper = ObjectMapper()
    private val retryDelayRegex = Regex("\"retryDelay\"\\s*:\\s*\"(\\d+(?:\\.\\d+)?)s\"")
    private val fenceRegex = Regex("^```[a-zA-Z]*\\s*\\n(.*?)\\n?```\\s*$", RegexOption.DOT_MATCHES_ALL)

    const val SYSTEM_PROMPT =
        "You are a professional literary translator of web novels. Translate the HTML fragment " +
            "you receive into natural, fluent Spanish. Keep every HTML tag, attribute and the " +
            "document structure exactly as they are and translate only the visible text. " +
            "Keep character and place names consistent. Do not add notes, explanations or " +
            "code fences: answer with the translated HTML only."

    sealed class Reply {
        data class Text(val text: String) : Reply()
        data class Blocked(val reason: String) : Reply()
    }

    fun endpoint(model: String) =
        "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

    /** Body of a generateContent request that translates [chunk]. */
    fun buildRequest(chunk: String): String {
        val root = mapper.createObjectNode()
        root.putObject("systemInstruction").putArray("parts").addObject().put("text", SYSTEM_PROMPT)
        val content = root.putArray("contents").addObject()
        content.put("role", "user")
        content.putArray("parts").addObject().put("text", chunk)
        root.putObject("generationConfig").put("temperature", 0.3)
        return mapper.writeValueAsString(root)
    }

    fun parseReply(json: String): Reply {
        val root = mapper.readTree(json)
        val candidate = root.path("candidates").path(0)
        val text = candidate.path("content").path("parts")
            .joinToString("") { it.path("text").asText("") }
        if (text.isBlank()) {
            val reason = root.path("promptFeedback").path("blockReason").asText("")
                .ifBlank { candidate.path("finishReason").asText("") }
                .ifBlank { "EMPTY" }
            return Reply.Blocked(reason)
        }
        return Reply.Text(stripCodeFence(text))
    }

    /** Removes the ```html fence models like to wrap their answer in. */
    fun stripCodeFence(text: String): String {
        val trimmed = text.trim()
        return fenceRegex.find(trimmed)?.groupValues?.get(1)?.trim() ?: trimmed
    }

    /** Message of the `error` object in an error answer, if any. */
    fun errorMessage(json: String): String? {
        val message = runCatching { mapper.readTree(json).path("error").path("message").asText("") }
        return message.getOrNull()?.takeIf { it.isNotBlank() }
    }

    /** Delay the server asked for in a 429 answer, in milliseconds. */
    fun retryDelayMs(json: String): Long? {
        val seconds = retryDelayRegex.find(json)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        return (seconds * 1000).toLong()
    }

    /** Splits a chapter into pieces of at most [maxChars] characters, cutting between top level nodes. */
    fun splitHtml(html: String, maxChars: Int = MAX_CHUNK_CHARS): List<String> {
        val nodes = Jsoup.parseBodyFragment(html).body().childNodes()
            .map { it.outerHtml() }
            .filter { it.isNotBlank() }
        val chunks = ArrayList<String>()
        val current = StringBuilder()
        for (node in nodes) {
            if (current.isNotEmpty() && current.length + node.length > maxChars) {
                chunks.add(current.toString())
                current.clear()
            }
            current.append(node).append('\n')
        }
        if (current.isNotEmpty()) chunks.add(current.toString())
        return chunks
    }
}

/** Calls the Gemini REST API through the app HTTP client. */
object DefaultGeminiTransport : GeminiTransport {
    override suspend fun post(model: String, apiKey: String, body: String): GeminiHttpResponse {
        return try {
            val response = app.post(
                GeminiProtocol.endpoint(model),
                headers = mapOf("x-goog-api-key" to apiKey),
                requestBody = body.toRequestBody("application/json".toMediaType()),
                timeout = 120L
            )
            GeminiHttpResponse(response.code, response.text)
        } catch (e: IOException) {
            GeminiHttpResponse(0, e.message.orEmpty())
        }
    }
}

/**
 * Translates chapters to Spanish with Gemini. Requests are spaced out so the free tier
 * rate limit is respected, and temporary errors are retried.
 */
class GeminiTranslator(
    private val apiKey: String,
    private val model: String,
    private val transport: GeminiTransport = DefaultGeminiTransport,
    private val minIntervalMs: Long = 4500L,
    private val maxRetries: Int = 4,
    private val sleep: suspend (Long) -> Unit = { delay(it) },
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var lastRequestAt = 0L

    /** Translates a chapter, the title travels with the first piece of text to save requests. */
    suspend fun translateChapter(chapter: LoadedChapter): LoadedChapter {
        val titleHtml = "<p class=\"${GeminiProtocol.TITLE_CLASS}\">${Entities.escape(chapter.title)}</p>\n"
        val chunks = GeminiProtocol.splitHtml(chapter.html).toMutableList()
        if (chunks.isEmpty()) chunks.add("")
        chunks[0] = titleHtml + chunks[0]

        val translated = chunks.map { translateChunk(it) }.toMutableList()
        val first = Jsoup.parseBodyFragment(translated[0])
        first.outputSettings().prettyPrint(false)
        val titleNode = first.body().selectFirst("p.${GeminiProtocol.TITLE_CLASS}")
        val title = titleNode?.text()?.takeIf { it.isNotBlank() } ?: chapter.title
        titleNode?.remove()
        translated[0] = first.body().html()
        return LoadedChapter(title, translated.joinToString("\n").trim())
    }

    /** Translates a short piece of HTML, used by the "test key" action as well. */
    suspend fun translateChunk(chunk: String): String {
        val body = GeminiProtocol.buildRequest(chunk)
        var attempt = 0
        while (true) {
            val wait = lastRequestAt + minIntervalMs - now()
            if (wait > 0) sleep(wait)
            val response = transport.post(model, apiKey, body)
            lastRequestAt = now()

            when (response.code) {
                200 -> return when (val reply = GeminiProtocol.parseReply(response.body)) {
                    is GeminiProtocol.Reply.Text -> reply.text
                    // The text was refused, keep the original so the chapter is still complete
                    is GeminiProtocol.Reply.Blocked -> chunk
                }

                0, 429, 500, 502, 503, 504 -> {
                    if (attempt >= maxRetries) {
                        throw TranslationException(failureMessage(response))
                    }
                    val retryAfter = GeminiProtocol.retryDelayMs(response.body)
                        ?: (10_000L shl attempt)
                    sleep(retryAfter.coerceAtMost(120_000L))
                    attempt++
                }

                else -> throw TranslationException(failureMessage(response))
            }
        }
    }

    /** Sends a tiny request to verify the key and model, returns the error text or null when it works. */
    suspend fun check(): String? {
        return try {
            translateChunk("<p>Hello</p>")
            null
        } catch (e: TranslationException) {
            e.message
        }
    }

    private fun failureMessage(response: GeminiHttpResponse): String {
        val detail = GeminiProtocol.errorMessage(response.body) ?: response.body.take(200)
        return "Gemini (${response.code}): $detail"
    }
}
