package com.subtitleextender.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Reads and writes SRT files through the Storage Access Framework.
 *
 * No storage permissions are required: every read or write operates on a
 * `content://` [Uri] the user explicitly picked via the system document
 * picker (`ACTION_OPEN_DOCUMENT` / `ACTION_CREATE_DOCUMENT`), which is the
 * scoped-storage-compliant way to access arbitrary files on modern Android.
 *
 * All I/O runs on [Dispatchers.IO] so callers never block the calling
 * coroutine's original dispatcher, keeping the UI thread free.
 */
class SubtitleRepository(private val context: Context) {

    /** Reads the full text contents of the document at [uri] as UTF-8. */
    suspend fun readTextFile(uri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open the selected file")

        inputStream.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        }
    }

    /** Writes [content] as UTF-8 text to the document at [uri], replacing any existing content. */
    suspend fun writeTextFile(uri: Uri, content: String): Unit = withContext(Dispatchers.IO) {
        // "wt" (write + truncate) ensures we fully replace the destination
        // file's contents rather than appending to or leaving stray bytes
        // after a shorter result, which matters if the user re-saves over
        // an existing file.
        val outputStream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Unable to open the destination file for writing")

        outputStream.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }

    /**
     * Returns the display name (e.g. "movie.srt") of the document at [uri],
     * or `null` if the provider does not expose one.
     */
    fun queryDisplayName(uri: Uri): String? {
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        ) ?: return null

        cursor.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (columnIndex >= 0) {
                    return it.getString(columnIndex)
                }
            }
        }
        return null
    }
}
