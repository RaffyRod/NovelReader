package com.lagradost.quicknovel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.lagradost.quicknovel.BaseApplication.Companion.getKey
import com.lagradost.quicknovel.BaseApplication.Companion.setKey
import java.io.File

/** One finished background translation / translated export the user can reopen. */
data class TranslationHistoryEntry(
    val novelName: String = "",
    val fileName: String = "",
    val uri: String = "",
    val mime: String = FileHelper.EPUB_MIME,
    val langCode: String = "",
    val time: Long = 0L,
    /** Parent folder / storage tree URI so “open path” can jump there. */
    val folderUri: String = "",
) {
    fun toUri(): Uri = uri.toUri()
    fun folderOrNull(): Uri? = folderUri.trim().takeIf { it.isNotBlank() }?.toUri()
}

/** Pure list rules for the global translation history (unit-tested). */
object TranslationHistoryLogic {
    const val MAX = 80

    fun add(current: List<TranslationHistoryEntry>, entry: TranslationHistoryEntry): List<TranslationHistoryEntry> =
        (listOf(entry) + current.filter { it.uri != entry.uri }).take(MAX)

    fun remove(current: List<TranslationHistoryEntry>, uri: String): List<TranslationHistoryEntry> =
        current.filter { it.uri != uri }

    fun keepExisting(
        current: List<TranslationHistoryEntry>,
        exists: (TranslationHistoryEntry) -> Boolean,
    ): List<TranslationHistoryEntry> = current.filter(exists)
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
        folderUri: Uri? = null,
    ) {
        val entry = TranslationHistoryEntry(
            novelName = novelName,
            fileName = fileName,
            uri = uri.toString(),
            mime = mime,
            langCode = langCode,
            time = System.currentTimeMillis(),
            folderUri = folderUri?.toString().orEmpty(),
        )
        setKey(KEY, TranslationHistoryLogic.add(load(), entry).toTypedArray())
    }

    fun forget(uri: String) {
        setKey(KEY, TranslationHistoryLogic.remove(load(), uri).toTypedArray())
    }

    fun available(context: Context): List<TranslationHistoryEntry> {
        val current = load()
        val kept = TranslationHistoryLogic.keepExisting(current) { exists(context, it.toUri()) }
        if (kept.size != current.size) {
            setKey(KEY, kept.toTypedArray())
        }
        return kept
    }

    fun open(context: Context, entry: TranslationHistoryEntry) {
        val viewUri = shareableUri(context, entry.toUri())
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(viewUri, entry.mime.ifBlank { "*/*" })
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK,
            )
        }
        context.startActivity(Intent.createChooser(intent, entry.fileName))
    }

    /**
     * Opens the download folder (or a document picker rooted there). Falls back to opening the file.
     */
    fun openFolder(context: Context, entry: TranslationHistoryEntry) {
        val folder = entry.folderOrNull() ?: parentOfFileUri(entry.toUri())
        if (folder != null) {
            val dirIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(folder, DocumentsContract.Document.MIME_TYPE_DIR)
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK,
                )
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, folder)
            }
            val treeIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, folder)
            }
            runCatching { context.startActivity(dirIntent) }
                .recoverCatching { context.startActivity(treeIntent) }
                .recoverCatching { open(context, entry) }
                .getOrThrow()
            return
        }
        open(context, entry)
    }

    private fun shareableUri(context: Context, uri: Uri): Uri {
        if (uri.scheme == "file") {
            val path = uri.path ?: return uri
            val file = File(path)
            if (file.exists()) {
                return FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".provider",
                    file,
                )
            }
        }
        return FileHelper.exportUri(context, uri)
    }

    private fun parentOfFileUri(uri: Uri): Uri? {
        if (uri.scheme != "file") return null
        val parent = uri.path?.let { File(it).parentFile } ?: return null
        return Uri.fromFile(parent)
    }

    private fun exists(context: Context, uri: Uri): Boolean {
        if (uri.scheme == "file") {
            return uri.path?.let { File(it).exists() } == true
        }
        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
        }.recoverCatching {
            // Directories / tree URIs may not open as "r" files.
            DocumentsContract.isDocumentUri(context, uri)
        }.getOrDefault(false)
    }
}
