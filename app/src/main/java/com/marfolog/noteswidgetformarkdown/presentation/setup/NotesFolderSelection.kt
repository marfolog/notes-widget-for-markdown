package com.marfolog.noteswidgetformarkdown.presentation.setup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.marfolog.noteswidgetformarkdown.data.repository.FolderDiagnoser
import com.marfolog.noteswidgetformarkdown.presentation.widget.NotesWidget
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import com.marfolog.noteswidgetformarkdown.worker.NotesFolderObserverJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Taking a picked folder as the notes folder, the same way from settings and from the first-launch guide. */
internal object NotesFolderSelection {

    private const val READ_WRITE = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    /**
     * Keeps access to [uri], gives the previous folder's back, re-arms the watcher and redraws the
     * widget. Returns the path below the vault root when that root was picked, null otherwise.
     * What the folder holds is looked up afterwards and reported as `folder_selected`.
     */
    fun select(context: Context, uri: Uri, scope: CoroutineScope): String? {
        val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(SetupActivity.KEY_NOTES_URI, null)?.let { old ->
            runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(old), READ_WRITE) }
        }
        context.contentResolver.takePersistableUriPermission(uri, READ_WRITE)

        val uriStr = uri.toString()
        prefs.edit().putString(SetupActivity.KEY_NOTES_URI, uriStr).apply()
        AppLog.i(SetupActivity.AREA, "Notes folder granted: $uriStr")
        // The watcher is bound to one specific tree uri.
        NotesFolderObserverJob.schedule(context)
        // The watcher only wakes when a file changes, so after a plain folder switch the widget
        // kept showing the old folder until something else redrew it.
        FolderDiagnoser.forget(uriStr)
        scope.launch { NotesWidget.updateAll(context) }
        // What the folder holds is looked up apart from the redraw, which must not wait for it.
        scope.launch {
            val diagnosis = FolderDiagnoser(context).diagnose(uriStr)
            Telemetry.event(UsageEvent.FOLDER_SELECTED, *UsageEvent.folderSelected(diagnosis))
        }

        val path = prefs.getString(SetupActivity.KEY_VAULT_URI, null)?.let { relativePath(it, uriStr) }
        if (path != null) prefs.edit().putString(SetupActivity.KEY_NOTE_FOLDER_PATH, path).apply()
        return path
    }

    fun relativePath(vaultUri: String, notesUri: String): String {
        val vaultDocId = DocumentsContract.getTreeDocumentId(Uri.parse(vaultUri))
        val notesDocId = DocumentsContract.getTreeDocumentId(Uri.parse(notesUri))
        return notesDocId.removePrefix(vaultDocId).removePrefix("/")
    }
}
