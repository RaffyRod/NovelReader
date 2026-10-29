package com.lagradost.quicknovel

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.lagradost.quicknovel.BaseApplication.Companion.getKey
import com.lagradost.quicknovel.BaseApplication.Companion.setKey

/** A book file (EPUB or PDF) the app saved for a novel, kept so it can be opened again later. */
data class ExportedFile(
    val name: String = "",
    val uri: String = "",
    val mime: String = FileHelper.EPUB_MIME,
    val time: Long = 0L,
) {
    val isPdf: Boolean get() = mime == FileHelper.PDF_MIME
    fun toUri(): Uri = uri.toUri()
}

/** Pure list rules, kept apart from storage so they can be unit tested. */
object ExportedFilesLogic {
    const val MAX_PER_NOVEL = 100

    /** Newest first, one entry per uri, so saving the same file again only moves it to the top. */
    fun add(
        current: List<ExportedFile>,
        entry: ExportedFile,
        limit: Int = MAX_PER_NOVEL
    ): List<ExportedFile> =
        (listOf(entry) + current.filter { it.uri != entry.uri }).take(limit)

    fun remove(current: List<ExportedFile>, uri: String): List<ExportedFile> =
        current.filter { it.uri != uri }
}

/** Remembers the files saved for each novel. */
object ExportedFiles {
    private const val FOLDER = "exported_files"

    fun list(novelName: String): List<ExportedFile> =
        getKey<Array<ExportedFile>>(FOLDER, novelName)?.toList() ?: emptyList()

    fun record(novelName: String, name: String, uri: Uri, mime: String) {
        val entry = ExportedFile(name, uri.toString(), mime, System.currentTimeMillis())
        setKey(FOLDER, novelName, ExportedFilesLogic.add(list(novelName), entry).toTypedArray())
    }

    /** Files of the novel that are still on the phone, forgetting the ones that were deleted. */
    fun available(context: Context, novelName: String): List<ExportedFile> {
        val (present, missing) = list(novelName).partition { exists(context, it.toUri()) }
        missing.forEach { forget(novelName, it.toUri()) }
        return present
    }

    private fun exists(context: Context, uri: Uri): Boolean =
        runCatching { context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false }
            .getOrDefault(false)

    /** Forgets a file that can no longer be opened, for example one deleted from the phone. */
    fun forget(novelName: String, uri: Uri) {
        setKey(FOLDER, novelName, ExportedFilesLogic.remove(list(novelName), uri.toString()).toTypedArray())
    }
}
