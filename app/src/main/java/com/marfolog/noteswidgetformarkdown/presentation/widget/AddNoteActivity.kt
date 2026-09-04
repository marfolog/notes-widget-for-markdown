package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.data.repository.ObsidianVaultLocator
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.usecase.CreateNoteUseCase
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.AppLog
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get

/**
 * Behind the + button on the widget.
 *
 * The previous approach asked Obsidian to create the note, with a path computed relative to the
 * vault root. When the vault could not be located — which SAF makes easy to happen, since it
 * cannot see above a granted folder — that path came out wrong and the note landed at the vault
 * root instead of the folder the widget reads. Creating the file ourselves, directly in the
 * folder we already hold write access to, cannot land anywhere else: it is the same folder
 * either way. Opening it afterwards goes through the usual [noteOpenIntent], so it still respects
 * the Obsidian / default app / ask-every-time setting.
 */
class AddNoteActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
        val folderUri = prefs.getString(SetupActivity.KEY_NOTES_URI, null)
            ?: prefs.getString(SetupActivity.KEY_VAULT_URI, null)
        if (folderUri == null) {
            AppLog.w(AREA, "No folder configured, nothing to create the note in")
            finish()
            return
        }

        lifecycleScope.launch {
            val title = uniqueTitle(folderUri)
            val createNoteUseCase = get<CreateNoteUseCase>(CreateNoteUseCase::class.java)
            val created = createNoteUseCase(folderUri, title, "")

            created.onSuccess { ok ->
                if (!ok) {
                    AppLog.w(AREA, "Note creation returned false")
                    finish()
                    return@onSuccess
                }
                openNewNote(folderUri, title)
            }.onFailure { error ->
                AppLog.e(AREA, "Failed to create note", error)
                Toast.makeText(this@AddNoteActivity, R.string.new_note_failed, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * `New Note.md`, then `New Note 2.md`, `New Note 3.md`… SAF providers vary in how they
     * handle a name collision — some silently pick their own suffix, some overwrite. Checking
     * first means the widget never overwrites something that was already there.
     */
    private fun uniqueTitle(folderUri: String): String {
        val folder = DocumentFile.fromTreeUri(this, Uri.parse(folderUri))
        val existingNames = folder?.listFiles()?.mapNotNull { it.name }.orEmpty().toSet()
        val base = getString(R.string.widget_new_note_name)
        if ("$base.md" !in existingNames) return base
        var n = 2
        while ("$base $n.md" in existingNames) n++
        return "$base $n"
    }

    private suspend fun openNewNote(folderUri: String, title: String) {
        val folder = DocumentFile.fromTreeUri(this, Uri.parse(folderUri))
        val note = folder?.findFile("$title.md")
        if (note == null) {
            AppLog.w(AREA, "Created note not found right after creating it")
            finish()
            return
        }
        val summary = NoteSummary(
            id = note.uri.toString(),
            title = title,
            preview = "",
            fileUri = note.uri.toString(),
            fileName = "$title.md",
            lastModified = note.lastModified()
        )

        // Vault name and folder path only decide the Obsidian deep link, which is one of three
        // ways this can open — the file itself is already in the right place regardless of
        // whether this lookup succeeds.
        val prefs = getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
        val detectedVault = ObsidianVaultLocator(this).locate()
        val vaultName = detectedVault?.name ?: prefs.getString(SetupActivity.KEY_VAULT_NAME, null)
        val noteFolderPath = detectedVault
            ?.let { ObsidianVaultLocator.relativePath(it.documentId, folderUri) }
            ?: prefs.getString(SetupActivity.KEY_NOTE_FOLDER_PATH, null)

        runCatching {
            startActivity(noteOpenIntent(this, summary, vaultName, noteFolderPath))
        }.onFailure { e ->
            if (e is ActivityNotFoundException) {
                Toast.makeText(this, R.string.no_app_opens_notes, Toast.LENGTH_LONG).show()
            } else {
                AppLog.w(AREA, "Could not open the new note", e)
            }
        }

        NotesWidget.updateAll(this)
        finish()
    }

    private companion object {
        const val AREA = "AddNote"
    }
}
