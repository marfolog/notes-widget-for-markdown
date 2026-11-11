package com.marfolog.noteswidgetformarkdown.presentation.setup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import java.io.File

/**
 * The folder for someone with no notes on the phone yet: `Documents/Notes` with one example note, so
 * the widget has something to show. It is made through MediaStore, which needs no storage permission;
 * the user then confirms the folder in the system picker like any other, so nothing after that differs.
 */
internal object StarterFolder {

    const val FOLDER_NAME = "Notes"
    const val RELATIVE_PATH = "Documents/Notes/"
    const val FILE_NAME = "Welcome to Notes Widget.md"
    private const val MIME_TYPE = "text/markdown"
    private const val STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    private const val AREA = "StarterFolder"

    /** Where the picker opens to make the starter folder the notes folder. */
    fun pickerStart(): Uri =
        DocumentsContract.buildDocumentUri(STORAGE_AUTHORITY, "primary:Documents/$FOLDER_NAME")

    /** Where the picker opens when the starter folder could not be made: the folder above it. */
    fun fallbackPickerStart(): Uri =
        DocumentsContract.buildDocumentUri(STORAGE_AUTHORITY, "primary:Documents")

    /**
     * Whether to make the example note. Only in a folder that is not there yet: without a storage
     * permission the app cannot see files it did not make, so a folder that exists may well hold
     * someone's notes and is left exactly as it is.
     */
    fun shouldCreateNote(folderExists: Boolean): Boolean = !folderExists

    /**
     * A name the media store gave back that is not the one asked for means the name was taken (it
     * appends "(1)"); the copy is then not wanted. Checked right after inserting, as the name is
     * settled then; an item marked pending would only be renamed once it is published.
     */
    fun isDuplicate(requested: String, given: String?): Boolean = given != requested

    /**
     * Makes sure the folder holds a note and returns one of the `STARTER_*` results. Blocking: call it
     * off the main thread. [body] is the text of the example note.
     */
    fun prepare(context: Context, body: String): String = try {
        if (!shouldCreateNote(folderExists(context))) {
            UsageEvent.STARTER_EXISTING
        } else {
            create(context, body)
        }
    } catch (e: Exception) {
        AppLog.w(AREA, "The starter folder could not be made", e)
        UsageEvent.STARTER_FAILED
    }

    /** The example note's text in the app's language. */
    fun noteText(context: Context): String = context.getString(R.string.starter_note_text)

    private fun create(context: Context, body: String): String {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(
            collection,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME)
                put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE)
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
            }
        ) ?: return UsageEvent.STARTER_FAILED
        try {
            val given = resolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
            if (isDuplicate(FILE_NAME, given)) {
                // The name exists already, from before this app's data was cleared or from another app.
                resolver.delete(uri, null, null)
                return UsageEvent.STARTER_EXISTING
            }
            val stream = resolver.openOutputStream(uri, "wt") ?: error("No stream for the starter note")
            stream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            return UsageEvent.STARTER_CREATED
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }

    /**
     * Whether `Documents/Notes` is there. The folder itself can be seen without any permission, its
     * files only when this app made them, so both are asked: after the app's data was cleared its
     * own note no longer counts as its own, and the folder is what still gives it away.
     */
    private fun folderExists(context: Context): Boolean {
        @Suppress("DEPRECATION")
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), FOLDER_NAME)
        if (runCatching { folder.isDirectory }.getOrDefault(false)) return true
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        return runCatching {
            context.contentResolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
                arrayOf(RELATIVE_PATH),
                null
            )?.use { it.count > 0 } ?: false
        }.getOrDefault(false)
    }
}
