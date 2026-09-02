package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import com.marfolog.noteswidgetformarkdown.data.preferences.NoteCardSettingsStore
import com.marfolog.noteswidgetformarkdown.data.repository.GitSyncStatusReader
import com.marfolog.noteswidgetformarkdown.data.repository.ObsidianVaultLocator
import com.marfolog.noteswidgetformarkdown.domain.model.GitSyncStatus
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardAppearance
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.usecase.GetNotesUseCase
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.AppLog
import kotlinx.coroutines.flow.firstOrNull
import org.koin.java.KoinJavaComponent.get

internal sealed interface WidgetState {
    data object Uninitialized : WidgetState
    data object PermissionLost : WidgetState
    data class Success(val notes: List<NoteSummary>) : WidgetState
    data object Empty : WidgetState
    data class Error(val message: String) : WidgetState
}

/** Everything a widget needs to render, so both widget flavours read the vault identically. */
internal data class WidgetData(
    val state: WidgetState,
    val vaultName: String?,
    val noteFolderPath: String?,
    val cardSettings: Map<String, NoteCardAppearance>,
    val gitSyncStatus: GitSyncStatus?
)

internal suspend fun loadWidgetData(context: Context): WidgetData {
    val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
    val folderUri = prefs.getString(SetupActivity.KEY_NOTES_URI, null)
        ?: prefs.getString(SetupActivity.KEY_VAULT_URI, null)

    // Obsidian resolves notes by vault name + path relative to the vault root. The picked
    // folder is often a subfolder, so prefer the detected vault over the stored names.
    val detectedVault = ObsidianVaultLocator(context).locate()
    val vaultName = detectedVault?.name ?: prefs.getString(SetupActivity.KEY_VAULT_NAME, null)
    val noteFolderPath = detectedVault
        ?.let { ObsidianVaultLocator.relativePath(it.documentId, folderUri) }
        ?: prefs.getString(SetupActivity.KEY_NOTE_FOLDER_PATH, null)

    val cardSettingsStore = NoteCardSettingsStore(context)
    // Jak uzivatel synchronizuje. Stara volba "zobrazovat cip" se prevede na rezim, at se
    // nikomu po aktualizaci nic nezmeni pod rukama.
    val syncMode = prefs.getString(SetupActivity.KEY_SYNC_MODE, null)
        ?: if (prefs.getBoolean(SetupActivity.KEY_SHOW_GIT_STATUS, true)) "git" else "none"

    val state: WidgetState = if (folderUri.isNullOrEmpty()) {
        WidgetState.Uninitialized
    } else {
        runCatching {
            val getNotesUseCase = get<GetNotesUseCase>(GetNotesUseCase::class.java)
            val notes = cardSettingsStore.applyOrder(
                getNotesUseCase(folderUri).firstOrNull() ?: emptyList()
            )
            if (notes.isEmpty()) WidgetState.Empty else WidgetState.Success(notes)
        }.getOrElse { e ->
            val message = e.message ?: "Unknown error"
            if (message.contains("permission", ignoreCase = true) || e is SecurityException) {
                WidgetState.PermissionLost
            } else {
                WidgetState.Error(message)
            }
        }
    }

    // Mimo git nemame co cist, tak vezmeme cas nejnovejsi poznamky. Az po nacteni seznamu,
    // aby se slozka nechodila prochazet dvakrat.
    val gitSyncStatus: GitSyncStatus? = when (syncMode) {
        "git" -> GitSyncStatusReader(context).read(prefs.getString(SetupActivity.KEY_VAULT_URI, null))
        "other" -> (state as? WidgetState.Success)
            ?.notes
            ?.maxOfOrNull { it.lastModified }
            ?.takeIf { it > 0 }
            ?.let { GitSyncStatus.FileActivity(it) }
        else -> null
    }

    AppLog.d(
        "WidgetData",
        "vault=${vaultName ?: "?"} folder=${noteFolderPath ?: "?"} " +
            "state=${state::class.simpleName} git=${gitSyncStatus?.let { it::class.simpleName } ?: "off"}"
    )
    return WidgetData(
        state = state,
        vaultName = vaultName,
        noteFolderPath = noteFolderPath,
        cardSettings = cardSettingsStore.getAll(),
        gitSyncStatus = gitSyncStatus
    )
}
