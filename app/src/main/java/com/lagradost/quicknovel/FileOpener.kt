package com.lagradost.quicknovel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog

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
        val intent = Intent(activity, target).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(intent)
    }

    private fun openInOtherApp(activity: Activity, uri: Uri, mime: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(FileHelper.exportUri(activity, uri), mime)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
        }
        activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.open_with)))
    }
}
