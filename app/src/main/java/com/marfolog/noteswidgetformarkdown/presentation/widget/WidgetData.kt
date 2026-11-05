package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import com.marfolog.noteswidgetformarkdown.data.preferences.ExtraNotesStore
import com.marfolog.noteswidgetformarkdown.data.preferences.NoteCardSettingsStore
import com.marfolog.noteswidgetformarkdown.data.repository.GitSyncStatusReader
import com.marfolog.noteswidgetformarkdown.data.repository.ObsidianVaultLocator
import com.marfolog.noteswidgetformarkdown.domain.model.GitSyncStatus
import com.marfolog.noteswidgetformarkdown.domain.model.CardAppearances
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.usecase.GetNotesUseCase
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.util.RateReminder
import com.marfolog.noteswidgetformarkdown.util.SyncProblemReport
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
    val cardAppearances: CardAppearances,
    val gitSyncStatus: GitSyncStatus?,
    /** Vault-relative folder of each extra note inside the vault, keyed by its uri. */
    val extraNoteFolders: Map<String, String> = emptyMap(),
    val bottomBarChip: BottomBarChip = BottomBarChip.None,
    val showAddButtons: Boolean = true
)

/**
 * Last data either widget flavour has successfully shown, kept outside any one Glance session.
 *
 * Every refresh now deliberately kills and restarts the session (see forceFreshGlanceSession),
 * which throws away whatever a `remember` inside provideContent was holding. Without this, that
 * meant every single refresh — even a sub-second one — showed a full loading screen, because the
 * new composition genuinely had no data to show yet. Holding the last result here lets a fresh
 * composition start from what was already on screen and reload quietly in the background instead.
 */
internal object WidgetDataCache {
    @Volatile
    var last: WidgetData? = null
}

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
                getNotesUseCase(folderUri, ExtraNotesStore(context).getAll()).firstOrNull() ?: emptyList()
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
        // Extra notes live outside the synced folder, so they say nothing about the sync.
        "other" -> (state as? WidgetState.Success)
            ?.notes
            ?.filterNot { it.isExtra }
            ?.maxOfOrNull { it.lastModified }
            ?.takeIf { it > 0 }
            ?.let { GitSyncStatus.FileActivity(it) }
        else -> null
    }

    // Worked out once here rather than per tap: each extra note can sit in a different folder of
    // the vault, or outside it altogether.
    val extraNoteFolders = detectedVault?.let { vault ->
        (state as? WidgetState.Success)?.notes
            ?.filter { it.isExtra }
            ?.mapNotNull { note ->
                ObsidianVaultLocator.folderOfNote(vault, note.fileUri)?.let { note.fileUri to it }
            }
            ?.toMap()
    }.orEmpty()

    val now = System.currentTimeMillis()
    val syncLabel = gitSyncStatus?.let {
        GitSyncLabel.format(
            it,
            now,
            prefs.getInt(SetupActivity.KEY_STALE_HOURS, GitSyncLabel.DEFAULT_STALE_HOURS)
        )
    }
    if (syncLabel != null) SyncProblemReport.reportOnce(context, syncLabel.trouble)
    val rateChipWindow = RateReminder.openWindow(context, now).takeIf { state.isUsable() }
    val bottomBarChip = bottomBarChip(rateChipWindow != null, syncLabel)
    if (bottomBarChip is BottomBarChip.Rate) RateReminder.reportShownOnce(context, rateChipWindow)

    AppLog.d(
        "WidgetData",
        "vault=${vaultName ?: "?"} folder=${noteFolderPath ?: "?"} " +
            "state=${state::class.simpleName} git=${gitSyncStatus?.let { it::class.simpleName } ?: "off"}"
    )
    return WidgetData(
        state = state,
        vaultName = vaultName,
        noteFolderPath = noteFolderPath,
        cardAppearances = CardAppearances(cardSettingsStore.getAll(), CardDefaults.get(context)),
        gitSyncStatus = gitSyncStatus,
        extraNoteFolders = extraNoteFolders,
        bottomBarChip = bottomBarChip,
        showAddButtons = AddButtons.isShown(context)
    )
}

private fun WidgetState.isUsable(): Boolean =
    this !is WidgetState.Uninitialized && this !is WidgetState.PermissionLost
