package com.lagradost.quicknovel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.lagradost.quicknovel.BaseApplication.Companion.getKey
import com.lagradost.quicknovel.BaseApplication.Companion.setKey

/** One finished background translation / translated export the user can reopen. */
data class TranslationHistoryEntry(
    val novelName: String = "",
    val fileName: String = "",
    val uri: String = "",
    val mime: String = FileHelper.EPUB_MIME,
    val langCode: String = "",
    val time: Long = 0L,
) {
    fun toUri(): Uri = uri.toUri()
}

/** Pure list rules for the global translation history (unit-tested). */
object TranslationHistoryLogic {
    const val MAX = 80

    fun add(current: List<TranslationHistoryEntry>, entry: TranslationHistoryEntry): List<TranslationHistoryEntry> =
        (listOf(entry) + current.filter { it.uri != entry.uri }).take(MAX)

    fun remove(current: List<TranslationHistoryEntry>, uri: String): List<TranslationHistoryEntry> =
        current.filter { it.uri != uri }
}

/** Global “Historial de traducciones” — newest first, openable from Sections. */
object TranslationHistory {
    private const val KEY = "translation_history_v1"

    fun load(): List<TranslationHistoryEntry> =
        getKey<Array<TranslationHistoryEntry>>(KEY)?.toList()
            ?: getKey<List<TranslationHistoryEntry>>(KEY)
            ?: emptyList()

    fun record(
        novelName: String,
        fileName: String,
        uri: Uri,
        mime: String,
        langCode: String,
    ) {
        val entry = TranslationHistoryEntry(
            novelName = novelName,
            fileName = fileName,
            uri = uri.toString(),
            mime = mime,
            langCode = langCode,
            time = System.currentTimeMillis(),
        )
        setKey(KEY, TranslationHistoryLogic.add(load(), entry).toTypedArray())
    }

    fun forget(uri: String) {
        setKey(KEY, TranslationHistoryLogic.remove(load(), uri).toTypedArray())
    }

    fun available(context: Context): List<TranslationHistoryEntry> {
        val (present, missing) = load().partition { exists(context, it.toUri()) }
        missing.forEach { forget(it.uri) }
        return present
    }

    fun open(context: Context, entry: TranslationHistoryEntry) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(entry.toUri(), entry.mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, entry.fileName))
    }

    fun openFolder(context: Context, entry: TranslationHistoryEntry) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(entry.toUri(), entry.mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Best-effort: many OEMs open the file’s parent when VIEW is used with a document URI.
        runCatching { context.startActivity(Intent.createChooser(intent, entry.fileName)) }
    }

    private fun exists(context: Context, uri: Uri): Boolean =
        runCatching { context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false }
            .getOrDefault(false)
}
