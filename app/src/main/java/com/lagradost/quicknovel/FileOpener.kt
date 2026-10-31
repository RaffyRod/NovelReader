package com.lagradost.quicknovel

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.lagradost.quicknovel.CommonActivity.showToast
import com.lagradost.quicknovel.mvvm.logError

/** Where a book file is read: in this app or in any other reading app on the phone. */
enum class OpenMode {
    InApp,
    OtherApp
}

object FileOpener {
    /** Opens an EPUB or PDF with the reader chosen by [mode]. */
    fun open(activity: Activity, uri: Uri, mime: String, mode: OpenMode) {
        when (mode) {
            OpenMode.InApp -> openInApp(activity, uri, mime)
            OpenMode.OtherApp -> openInOtherApp(activity, uri, mime)
        }
    }

    /** Lets the user decide, each time, where the file will be read. */
    fun askAndOpen(activity: Activity, uri: Uri, mime: String, onCancel: () -> Unit = {}) {
        val options = arrayOf(
            activity.getString(R.string.open_in_app),
            activity.getString(R.string.open_in_other_app)
        )
        AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            .setTitle(R.string.open_with)
            .setItems(options) { _, which ->
                open(activity, uri, mime, if (which == 0) OpenMode.InApp else OpenMode.OtherApp)
            }
            .setNegativeButton(R.string.cancel) { _, _ -> onCancel() }
            .show()
    }

    private fun openInApp(activity: Activity, uri: Uri, mime: String) {
        val target = if (mime == FileHelper.PDF_MIME) PdfReaderActivity::class.java
        else ReadActivity2::class.java
        // Prefer a FileProvider URI for raw files so the reader always has read access.
        val readable = runCatching { FileHelper.exportUri(activity, uri) }.getOrDefault(uri)
        val intent = Intent(activity, target).apply {
            setDataAndType(readable, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Android only forwards URI grants to the next activity when ClipData is set.
            clipData = ClipData.newUri(activity.contentResolver, "book", readable)
        }
        runCatching { activity.startActivity(intent) }
            .onFailure { err ->
                logError(err)
                showToast(activity, activity.getString(R.string.open_file_failed))
            }
    }

    private fun openInOtherApp(activity: Activity, uri: Uri, mime: String) {
        val readable = runCatching { FileHelper.exportUri(activity, uri) }.getOrDefault(uri)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(readable, mime)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
            clipData = ClipData.newUri(activity.contentResolver, "book", readable)
        }
        runCatching {
            activity.startActivity(
                Intent.createChooser(intent, activity.getString(R.string.open_with))
            )
        }.onFailure { err ->
            logError(err)
            showToast(activity, activity.getString(R.string.open_file_failed))
        }
    }
}
