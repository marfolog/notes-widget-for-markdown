package com.marfolog.noteswidgetformarkdown.presentation.setup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.marfolog.noteswidgetformarkdown.BuildConfig
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.data.preferences.NoteCardSettingsStore
import com.marfolog.noteswidgetformarkdown.data.repository.GitSyncStatusReader
import com.marfolog.noteswidgetformarkdown.data.repository.ObsidianVaultLocator
import java.text.DateFormat
import java.util.Date
import com.marfolog.noteswidgetformarkdown.domain.model.GitSyncStatus
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardAppearance
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardColor
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardSize
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardTextSize
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.usecase.DeleteNoteUseCase
import com.marfolog.noteswidgetformarkdown.domain.usecase.GetNotesUseCase
import com.marfolog.noteswidgetformarkdown.presentation.widget.GitSyncLabel
import com.marfolog.noteswidgetformarkdown.presentation.widget.NotesWidget
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.worker.NotesFolderObserverJob
import com.marfolog.noteswidgetformarkdown.ui.theme.NoteCardPalette
import com.marfolog.noteswidgetformarkdown.ui.theme.NotesWidgetForMarkdownTheme
import com.marfolog.noteswidgetformarkdown.ui.theme.ThemePreference
import sh.calvin.reorderable.ReorderableColumn
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get

class SetupActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.load(this)
        setContent {
            NotesWidgetForMarkdownTheme(
                darkTheme = when (ThemePreference.mode) {
                    ThemePreference.Mode.Light -> false
                    ThemePreference.Mode.Dark -> true
                    ThemePreference.Mode.System -> isSystemInDarkTheme()
                }
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(stringResource(R.string.settings_title))
                                    // Verze primo v hlavicce: pri hlaseni chyby ji uzivatel
                                    // nemusi hledat v systemovem nastaveni.
                                    Text(
                                        text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    SetupScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    companion object {
        internal const val AREA = "Setup"
        const val PREFS_NAME = "app_prefs"
        const val KEY_GUIDE_DONE = "guide_done"
        const val KEY_SYNC_MODE = "sync_mode"
        const val KEY_STALE_HOURS = "sync_stale_hours"
        const val KEY_VAULT_URI = "vault_uri"
        const val KEY_NOTES_URI = "notes_uri"
        const val KEY_VAULT_NAME = "vault_name"
        const val KEY_NOTE_FOLDER_PATH = "note_folder_path"
        const val KEY_FAST_NOTE_TARGET_URI = "fast_note_target_uri"
        const val KEY_GIT_ROOT_URI = "git_root_uri"

        /** Users who sync by other means (Syncthing, Obsidian Sync…) can hide the chip. */
        const val KEY_SHOW_GIT_STATUS = "show_git_status"
    }
}

@Composable
private fun SetupScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()
    val cardSettingsStore = remember { NoteCardSettingsStore(context.applicationContext) }

    var vaultUri by rememberSaveable { mutableStateOf(prefs.getString(SetupActivity.KEY_VAULT_URI, null)) }
    var notesUri by rememberSaveable { mutableStateOf(prefs.getString(SetupActivity.KEY_NOTES_URI, null)) }
    var vaultName by rememberSaveable { mutableStateOf(prefs.getString(SetupActivity.KEY_VAULT_NAME, null)) }
    var derivedPath by rememberSaveable { mutableStateOf(prefs.getString(SetupActivity.KEY_NOTE_FOLDER_PATH, null) ?: "") }
    var fastNoteTargetUri by rememberSaveable { mutableStateOf(prefs.getString(SetupActivity.KEY_FAST_NOTE_TARGET_URI, null)) }
    var notesForSettings by remember { mutableStateOf<List<NoteSummary>>(emptyList()) }
    var cardSettings by remember { mutableStateOf(cardSettingsStore.getAll()) }
    var notesLoadError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(notesUri) {
        val currentNotesUri = notesUri
        if (currentNotesUri.isNullOrEmpty()) {
            notesForSettings = emptyList()
            notesLoadError = null
            return@LaunchedEffect
        }

        runCatching {
            val getNotesUseCase = get<GetNotesUseCase>(GetNotesUseCase::class.java)
            cardSettingsStore.applyOrder(getNotesUseCase(currentNotesUri).firstOrNull().orEmpty())
        }.onSuccess { notes ->
            notesForSettings = notes
            cardSettings = cardSettingsStore.getAll()
            notesLoadError = null
        }.onFailure { error ->
            AppLog.w(SetupActivity.AREA, "Could not list notes in $currentNotesUri", error)
            notesForSettings = emptyList()
            notesLoadError = error.message ?: context.getString(R.string.notes_load_failed)
        }
    }

    var expandedNoteUri by rememberSaveable { mutableStateOf<String?>(null) }
    var noteToDelete by remember { mutableStateOf<NoteSummary?>(null) }
    var gitStatus by remember { mutableStateOf<GitSyncStatus>(GitSyncStatus.NotTracked) }
    var telemetryEnabled by rememberSaveable { mutableStateOf(Telemetry.isEnabled(context)) }
    // Whether the app could work the vault out on its own. When it can, the root folder never
    // has to be picked, which is the difference between one tap and two.
    var detectedVaultName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(notesUri, vaultUri) {
        val vault = ObsidianVaultLocator(context.applicationContext).locate()
        detectedVaultName = vault
            ?.takeIf { ObsidianVaultLocator.relativePath(it.documentId, notesUri) != null }
            ?.name
    }
    var showGitStatus by rememberSaveable {
        mutableStateOf(prefs.getBoolean(SetupActivity.KEY_SHOW_GIT_STATUS, true))
    }
    // Stara volba "zobrazovat cip" se prevede na rezim, at se po aktualizaci nikomu nic nezmeni.
    var syncMode by rememberSaveable {
        mutableStateOf(
            prefs.getString(SetupActivity.KEY_SYNC_MODE, null)
                ?: if (showGitStatus) "git" else "none"
        )
    }
    var staleHours by rememberSaveable {
        mutableIntStateOf(
            prefs.getInt(SetupActivity.KEY_STALE_HOURS, GitSyncLabel.DEFAULT_STALE_HOURS)
        )
    }
    var gitReloadTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(vaultUri, notesUri, gitReloadTrigger) {
        gitStatus = GitSyncStatusReader(context.applicationContext).read(vaultUri)
    }

    /**
     * SAF cannot walk above a granted tree, so when `.git` sits one level up we have to ask
     * for that folder explicitly. We at least open the picker directly in the parent folder.
     */
    val gitPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            AppLog.i(SetupActivity.AREA, "Git folder granted: $uri")
            prefs.edit().putString(SetupActivity.KEY_GIT_ROOT_URI, uri.toString()).apply()
            gitReloadTrigger++
            scope.launch { NotesWidget.updateAll(context) }
        }
    }

    fun parentFolderHint(): Uri? = runCatching {
        val childTreeUri = Uri.parse(notesUri ?: vaultUri ?: return@runCatching null)
        val parentDocId = DocumentsContract.getTreeDocumentId(childTreeUri)
            .substringBeforeLast('/')
            .takeIf { it.contains(':') }
            ?: return@runCatching null
        DocumentsContract.buildDocumentUri(childTreeUri.authority, parentDocId)
    }.getOrNull()

    fun computeRelativePath(vaultUriStr: String, notesUriStr: String): String {
        val vaultDocId = DocumentsContract.getTreeDocumentId(Uri.parse(vaultUriStr))
        val notesDocId = DocumentsContract.getTreeDocumentId(Uri.parse(notesUriStr))
        return notesDocId.removePrefix(vaultDocId).removePrefix("/")
    }

    val vaultPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val oldUriString = prefs.getString(SetupActivity.KEY_VAULT_URI, null)
            if (oldUriString != null) {
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        Uri.parse(oldUriString),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }

            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            val name = DocumentFile.fromTreeUri(context, uri)?.name
            val uriStr = uri.toString()
            AppLog.i(SetupActivity.AREA, "Vault root granted: $uriStr")

            prefs.edit()
                .putString(SetupActivity.KEY_VAULT_URI, uriStr)
                .putString(SetupActivity.KEY_VAULT_NAME, name)
                .apply()

            vaultUri = uriStr
            vaultName = name

            // Recompute relative path if notes folder is already selected
            val currentNotesUri = notesUri
            if (currentNotesUri != null) {
                val path = computeRelativePath(uriStr, currentNotesUri)
                derivedPath = path
                prefs.edit().putString(SetupActivity.KEY_NOTE_FOLDER_PATH, path).apply()
            }
        }
    }

    val notesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val oldUriString = prefs.getString(SetupActivity.KEY_NOTES_URI, null)
            if (oldUriString != null) {
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        Uri.parse(oldUriString),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }

            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            val uriStr = uri.toString()

            prefs.edit()
                .putString(SetupActivity.KEY_NOTES_URI, uriStr)
                .apply()

            notesUri = uriStr
            AppLog.i(SetupActivity.AREA, "Notes folder granted: $uriStr")
            // Re-arm the folder watcher: it is bound to one specific tree URI.
            NotesFolderObserverJob.schedule(context)

            // Compute relative path if vault is already selected
            val currentVaultUri = vaultUri
            if (currentVaultUri != null) {
                val path = computeRelativePath(currentVaultUri, uriStr)
                derivedPath = path
                prefs.edit().putString(SetupActivity.KEY_NOTE_FOLDER_PATH, path).apply()
            }
        }
    }

    val vaultDisplayName = vaultName
    // Nazev musi byt neprazdny vzdy, kdyz je slozka vybrana — zbytek nastaveni se podle nej
    // zobrazuje. Kdyz na ni prijdeme o opravneni, DocumentFile.name vrati null, tak vezmeme
    // aspon posledni kus cesty.
    val notesDisplayName = notesUri?.let { uriString ->
        runCatching {
            DocumentFile.fromTreeUri(context, Uri.parse(uriString))?.name
        }.getOrNull() ?: Uri.decode(uriString.substringAfterLast('/')).substringAfterLast('/')
    }

    // Pruvodce: zadne cislo kroku se neuklada, krok se odvozuje z toho, co uz je nastaveno.
    // Diky tomu prezije otoceni displeje i navrat z file pickeru bez dalsiho stavu.
    var guideDone by rememberSaveable {
        mutableStateOf(prefs.getBoolean(SetupActivity.KEY_GUIDE_DONE, false))
    }
    // Kdo appku uz pouziva a jen ji aktualizoval, zadneho pruvodce nedostane.
    LaunchedEffect(Unit) {
        if (!guideDone && notesUri != null) {
            prefs.edit().putBoolean(SetupActivity.KEY_GUIDE_DONE, true).apply()
            guideDone = true
        }
    }
    val dismissGuide: () -> Unit = {
        prefs.edit().putBoolean(SetupActivity.KEY_GUIDE_DONE, true).apply()
        guideDone = true
    }
    val guideStep: GuideStep? = when {
        guideDone -> null
        notesUri == null -> GuideStep.PickFolder
        else -> GuideStep.Save
    }

    val onSave: () -> Unit = {
        dismissGuide()
        Toast.makeText(context, R.string.widget_updated_toast, Toast.LENGTH_SHORT).show()

        scope.launch {
            NotesWidget.updateAll(context)
            (context as? Activity)?.finish()
        }
    }

    SetupScreenContent(
        guideStep = guideStep,
        onSkipGuide = dismissGuide,
        vaultName = vaultDisplayName,
        onSelectVault = { vaultPicker.launch(null) },
        notesFolderName = notesDisplayName,
        notesFolderPath = notesUri?.let { humanFolderPath(it) },
        onSelectNotesFolder = { notesPicker.launch(null) },
        derivedPath = derivedPath,
        notes = notesForSettings,
        cardSettings = cardSettings,
        notesLoadError = notesLoadError,
        fastNoteTargetUri = fastNoteTargetUri,
        onFastNoteTargetSelected = { note ->
            fastNoteTargetUri = note.fileUri
            prefs.edit()
                .putString(SetupActivity.KEY_FAST_NOTE_TARGET_URI, note.fileUri)
                .commit()
        },
        onCardSizeSelected = { note, size ->
            val updated = (cardSettings[note.fileUri] ?: NoteCardAppearance()).copy(size = size)
            cardSettingsStore.save(note.fileUri, updated)
            cardSettings = cardSettingsStore.getAll()
            scope.launch { NotesWidget.updateAll(context) }
        },
        onCardColorSelected = { note, color ->
            val updated = (cardSettings[note.fileUri] ?: NoteCardAppearance()).copy(color = color)
            cardSettingsStore.save(note.fileUri, updated)
            cardSettings = cardSettingsStore.getAll()
            scope.launch { NotesWidget.updateAll(context) }
        },
        onCardTextSizeSelected = { note, textSize ->
            val updated = (cardSettings[note.fileUri] ?: NoteCardAppearance()).copy(textSize = textSize)
            cardSettingsStore.save(note.fileUri, updated)
            cardSettings = cardSettingsStore.getAll()
            scope.launch { NotesWidget.updateAll(context) }
        },
        onCardCustomColorChanged = { note, colorHex ->
            val updated = (cardSettings[note.fileUri] ?: NoteCardAppearance()).copy(
                color = NoteCardColor.Custom,
                customColorHex = colorHex
            )
            cardSettingsStore.save(note.fileUri, updated)
            cardSettings = cardSettingsStore.getAll()
            scope.launch { NotesWidget.updateAll(context) }
        },
        onReorder = { from, to ->
            if (from in notesForSettings.indices && to in notesForSettings.indices) {
                notesForSettings = notesForSettings.toMutableList().also { notes ->
                    notes.add(to, notes.removeAt(from))
                }
                cardSettingsStore.saveOrder(notesForSettings.map { it.fileUri })
                scope.launch { NotesWidget.updateAll(context) }
            }
        },
        expandedNoteUri = expandedNoteUri,
        onToggleExpanded = { note ->
            expandedNoteUri = if (expandedNoteUri == note.fileUri) null else note.fileUri
        },
        onDeleteRequested = { note -> noteToDelete = note },
        syncMode = syncMode,
        onSyncModeChanged = { mode ->
            syncMode = mode
            prefs.edit().putString(SetupActivity.KEY_SYNC_MODE, mode).apply()
            scope.launch { NotesWidget.updateAll(context) }
        },
        staleHours = staleHours,
        onStaleHoursChanged = { hours ->
            staleHours = hours
            prefs.edit().putInt(SetupActivity.KEY_STALE_HOURS, hours).apply()
            scope.launch { NotesWidget.updateAll(context) }
        },
        gitStatusText = gitStatusText(context, gitStatus),
        gitIsTracked = gitStatus is GitSyncStatus.Tracked,
        gitIsBroken = gitStatus is GitSyncStatus.Unavailable,
        onSelectGitFolder = { gitPicker.launch(parentFolderHint()) },
        detectedVaultName = detectedVaultName,
        telemetryAvailable = Telemetry.ENABLED,
        telemetryEnabled = telemetryEnabled,
        onTelemetryChanged = { enabled ->
            telemetryEnabled = enabled
            Telemetry.setEnabled(context, enabled)
            AppLog.i(SetupActivity.AREA, "Reporting switched ${if (enabled) "on" else "off"}")
        },
        showGitStatus = showGitStatus,
        onShowGitStatusChanged = { enabled ->
            showGitStatus = enabled
            prefs.edit().putBoolean(SetupActivity.KEY_SHOW_GIT_STATUS, enabled).apply()
            scope.launch { NotesWidget.updateAll(context) }
        },
        canSave = notesUri != null,
        onSave = onSave,
        modifier = modifier
    )

    // Deleting is permanent — SAF has no trash, and recreating the file would give it a new
    // URI, orphaning its appearance settings. So: confirm, then delete for real.
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text(stringResource(R.string.delete_note_title, note.fileName)) },
            text = { Text(stringResource(R.string.delete_note_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        noteToDelete = null
                        scope.launch {
                            val deleteNoteUseCase = get<DeleteNoteUseCase>(DeleteNoteUseCase::class.java)
                            AppLog.i(SetupActivity.AREA, "Deleting a note file on user request")
                            deleteNoteUseCase(note.fileUri)
                                .onSuccess { deleted ->
                                    if (deleted) {
                                        cardSettingsStore.forget(note.fileUri)
                                        if (fastNoteTargetUri == note.fileUri) {
                                            fastNoteTargetUri = null
                                            prefs.edit()
                                                .remove(SetupActivity.KEY_FAST_NOTE_TARGET_URI)
                                                .apply()
                                        }
                                        notesForSettings = notesForSettings.filterNot {
                                            it.fileUri == note.fileUri
                                        }
                                        cardSettings = cardSettingsStore.getAll()
                                        if (expandedNoteUri == note.fileUri) {
                                            expandedNoteUri = null
                                        }
                                        NotesWidget.updateAll(context)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Could not delete ${note.fileName}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .onFailure { error ->
                                    AppLog.e(SetupActivity.AREA, "Delete failed", error)
                                    Toast.makeText(
                                        context,
                                        error.message ?: context.getString(R.string.delete_note_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { noteToDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

/**
 * Ctelna cesta ke slozce ze SAF tree URI. Z "primary:Documents/vault/Quick" udela
 * "Internal storage/Documents/vault/Quick". Kdyz se to nepovede, vrati null a zobrazi se
 * aspon nazev slozky.
 */
private fun humanFolderPath(treeUri: String): String? = runCatching {
    val docId = DocumentsContract.getTreeDocumentId(Uri.parse(treeUri))
    val volume = docId.substringBefore(':')
    val path = docId.substringAfter(':', "")
    val root = if (volume.equals("primary", ignoreCase = true)) "Internal storage" else volume
    if (path.isEmpty()) root else "$root/$path"
}.getOrNull()

private fun gitStatusText(context: Context, status: GitSyncStatus): String = when (status) {
    is GitSyncStatus.NotTracked -> context.getString(R.string.git_status_not_tracked)
    is GitSyncStatus.Unavailable ->
        context.getString(R.string.git_status_unreadable, status.reason)
    is GitSyncStatus.FileActivity -> context.getString(
        R.string.git_status_file_activity,
        SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
            .format(Date(status.lastChangeAtMillis))
    )
    is GitSyncStatus.Tracked -> buildString {
        // This is when HEAD last moved — a pull, a commit, a checkout. Not "when the note
        // was written", and not "when the client last ran".
        append(context.getString(R.string.git_status_last_change))
        status.lastAction?.let { append(" ($it)") }
        append(": ")
        append(
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(status.lastChangeAtMillis))
        )
        status.branch?.let { append(context.getString(R.string.git_status_branch, it)) }
        status.lastFetchAtMillis?.let { fetchedAt ->
            append("\n")
            append(context.getString(R.string.git_status_last_remote_contact))
            append(
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(fetchedAt))
            )
        }
        // Two different questions: "when did anything arrive" vs "is the client even running".
        status.lastClientActivityAtMillis?.let { activeAt ->
            append("\n")
            append(context.getString(R.string.git_status_client_active))
            append(
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(activeAt))
            )
        }
        val problem = when (status.problem) {
            GitSyncStatus.Problem.None -> null
            GitSyncStatus.Problem.Conflict -> R.string.git_problem_conflict
            GitSyncStatus.Problem.RebaseInProgress -> R.string.git_problem_rebase
            GitSyncStatus.Problem.Diverged -> R.string.git_problem_diverged
        }
        problem?.let { append("\n\n⚠ " + context.getString(it)) }
    }
}

@Composable
internal fun SetupScreenContent(
    vaultName: String?,
    onSelectVault: () -> Unit,
    notesFolderName: String?,
    notesFolderPath: String? = null,
    onSelectNotesFolder: () -> Unit,
    derivedPath: String,
    notes: List<NoteSummary> = emptyList(),
    cardSettings: Map<String, NoteCardAppearance> = emptyMap(),
    notesLoadError: String? = null,
    fastNoteTargetUri: String? = null,
    onFastNoteTargetSelected: (NoteSummary) -> Unit = {},
    onCardSizeSelected: (NoteSummary, NoteCardSize) -> Unit = { _, _ -> },
    onCardColorSelected: (NoteSummary, NoteCardColor) -> Unit = { _, _ -> },
    onCardTextSizeSelected: (NoteSummary, NoteCardTextSize) -> Unit = { _, _ -> },
    onCardCustomColorChanged: (NoteSummary, String) -> Unit = { _, _ -> },
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    expandedNoteUri: String? = null,
    onToggleExpanded: (NoteSummary) -> Unit = {},
    onDeleteRequested: (NoteSummary) -> Unit = {},
    gitStatusText: String = "",
    gitIsTracked: Boolean = false,
    // Cervene se hlasi jen nalezeny, ale necitelny .git. Zadny git neni chyba — spousta lidi
    // synchronizuje jinak.
    gitIsBroken: Boolean = false,
    onSelectGitFolder: () -> Unit = {},
    showGitStatus: Boolean = true,
    onShowGitStatusChanged: (Boolean) -> Unit = {},
    syncMode: String = "git",
    onSyncModeChanged: (String) -> Unit = {},
    staleHours: Int = GitSyncLabel.DEFAULT_STALE_HOURS,
    onStaleHoursChanged: (Int) -> Unit = {},
    detectedVaultName: String? = null,
    telemetryAvailable: Boolean = false,
    telemetryEnabled: Boolean = false,
    onTelemetryChanged: (Boolean) -> Unit = {},
    canSave: Boolean,
    onSave: () -> Unit,
    guideStep: GuideStep? = null,
    onSkipGuide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        DriftingLogo(containerWidth = maxWidth, containerHeight = maxHeight)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
        if (guideStep != null) {
            GuideBanner(step = guideStep, onSkip = onSkipGuide)
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section 1: the only folder anyone has to pick
        Text(
            text = if (notesFolderName == null) stringResource(R.string.notes_folder_title_first) else stringResource(R.string.notes_folder_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.notes_folder_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (notesFolderName != null) {
            // Samotny nazev slozky rikal min nez cesta, ktera ho obsahuje taky.
            Text(
                text = notesFolderPath ?: notesFolderName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.notes_folder_plus_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (notesFolderName == null) {
            Button(
                onClick = onSelectNotesFolder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.notes_folder_select))
            }
        } else {
            OutlinedButton(onClick = onSelectNotesFolder, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.notes_folder_change))
            }
        }

        // Vsechno dalsi ma smysl az nad vybranou slozkou. Dokud neni, byla by to jen rada
        // zasedlych tlacitek a odstavcu o necem, co jeste neexistuje.
        if (notesFolderName != null) {

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.fast_note_target_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.fast_note_target_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        FastNoteTargetSection(
            notes = notes,
            notesLoadError = notesLoadError,
            selectedNoteUri = fastNoteTargetUri,
            onTargetSelected = onFastNoteTargetSelected,
            folderSelected = notesFolderName != null
        )

        Spacer(modifier = Modifier.height(32.dp))

        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Section 2: only needed when the vault could not be worked out
        if (detectedVaultName != null) {
            Text(
                text = stringResource(R.string.vault_detected, detectedVaultName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        } else {
        Text(
            text = stringResource(R.string.vault_root_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.vault_root_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (vaultName != null) {
            Text(
                text = vaultName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = onSelectVault,
            enabled = notesFolderName != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (vaultName != null) stringResource(R.string.vault_root_change) else stringResource(R.string.vault_root_select))
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        }

        // Only the Play build has anything to switch off; the foss build never reports.
        if (telemetryAvailable) {
            Text(
                text = stringResource(R.string.reporting_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.reporting_switch),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = telemetryEnabled, onCheckedChange = onTelemetryChanged)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.reporting_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section 3: Git sync status
        Text(
            text = stringResource(R.string.sync_section_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.sync_section_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.sync_question),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SYNC_MODES.forEach { (mode, labelRes) ->
                FilterChip(
                    selected = syncMode == mode,
                    onClick = { onSyncModeChanged(mode) },
                    label = { Text(stringResource(labelRes)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when (syncMode) {
                "git" -> stringResource(R.string.sync_desc_git)
                "other" -> stringResource(R.string.sync_desc_other)
                else -> stringResource(R.string.sync_no_chip)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (syncMode != "none") {
            Spacer(modifier = Modifier.height(12.dp))
            // "Nothing new" znamena v kazdem rezimu neco jineho, tak se to rovnou pojmenuje.
            Text(
                text = if (syncMode == "git") {
                    stringResource(R.string.sync_warn_git)
                } else {
                    stringResource(R.string.sync_warn_files)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (syncMode == "git") {
                    stringResource(R.string.sync_warn_git_hint)
                } else {
                    stringResource(R.string.sync_warn_files_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                STALE_CHOICES.forEach { (hours, labelRes) ->
                    FilterChip(
                        selected = staleHours == hours,
                        onClick = { onStaleHoursChanged(hours) },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (syncMode != "none") {
        Text(
            text = if (notesFolderName == null) {
                stringResource(R.string.sync_not_checked_yet)
            } else {
                gitStatusText
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (gitIsBroken) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (syncMode == "git") {
        OutlinedButton(
            onClick = onSelectGitFolder,
            enabled = notesFolderName != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (gitIsTracked) stringResource(R.string.git_folder_change) else stringResource(R.string.git_folder_select))
        }
        }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.cards_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.cards_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        CardSettingsSection(
            notes = notes,
            cardSettings = cardSettings,
            notesLoadError = notesLoadError,
            onCardSizeSelected = onCardSizeSelected,
            onCardColorSelected = onCardColorSelected,
            onCardTextSizeSelected = onCardTextSizeSelected,
            onCardCustomColorChanged = onCardCustomColorChanged,
            onReorder = onReorder,
            expandedNoteUri = expandedNoteUri,
            onToggleExpanded = onToggleExpanded,
            onDeleteRequested = onDeleteRequested
        )

        Spacer(modifier = Modifier.height(32.dp))

        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.appearance_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.appearance_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        val appearanceContext = LocalContext.current
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemePreference.Mode.entries.forEach { mode ->
                FilterChip(
                    selected = ThemePreference.mode == mode,
                    onClick = { ThemePreference.set(appearanceContext, mode) },
                    label = { Text(mode.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        AboutSection()

        Spacer(modifier = Modifier.height(32.dp))

        // Save button
        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_done))
        }

        Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FastNoteTargetSection(
    notes: List<NoteSummary>,
    notesLoadError: String?,
    selectedNoteUri: String?,
    onTargetSelected: (NoteSummary) -> Unit,
    folderSelected: Boolean = false
) {
    when {
        notesLoadError != null -> {
            Text(
                text = notesLoadError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        notes.isEmpty() -> {
            // Prazdna slozka a nevybrana slozka jsou dve ruzne situace a uzivatel v kazde dela
            // neco jineho.
            Text(
                text = if (folderSelected) {
                    stringResource(R.string.fast_note_target_folder_empty)
                } else {
                    stringResource(R.string.fast_note_target_none_yet)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            val selectedExists = selectedNoteUri == null || notes.any { it.fileUri == selectedNoteUri }
            if (!selectedExists) {
                Text(
                    text = stringResource(R.string.fast_note_target_gone),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                notes.forEach { note ->
                    FilterChip(
                        selected = note.fileUri == selectedNoteUri,
                        onClick = { onTargetSelected(note) },
                        label = { Text(note.title) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CardSettingsSection(
    notes: List<NoteSummary>,
    cardSettings: Map<String, NoteCardAppearance>,
    notesLoadError: String?,
    onCardSizeSelected: (NoteSummary, NoteCardSize) -> Unit,
    onCardColorSelected: (NoteSummary, NoteCardColor) -> Unit,
    onCardTextSizeSelected: (NoteSummary, NoteCardTextSize) -> Unit,
    onCardCustomColorChanged: (NoteSummary, String) -> Unit,
    onReorder: (Int, Int) -> Unit,
    expandedNoteUri: String?,
    onToggleExpanded: (NoteSummary) -> Unit,
    onDeleteRequested: (NoteSummary) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var hintPending by rememberSaveable { mutableStateOf(!prefs.getBoolean(KEY_SWIPE_HINT_SHOWN, false)) }
    val hintOffset = remember { Animatable(0f) }

    when {
        notesLoadError != null -> {
            Text(
                text = notesLoadError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        notes.isEmpty() -> {
            Text(
                text = stringResource(R.string.cards_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            // Prvni radek trikrat popojede do strany, at je gesto videt. Pak uz nikdy.
            LaunchedEffect(hintPending, notes.isEmpty()) {
                if (!hintPending || notes.isEmpty()) return@LaunchedEffect
                delay(600)
                repeat(3) {
                    hintOffset.animateTo(-64f, tween(260))
                    hintOffset.animateTo(0f, tween(260))
                    delay(120)
                }
                prefs.edit().putBoolean(KEY_SWIPE_HINT_SHOWN, true).apply()
                hintPending = false
            }
            ReorderableColumn(
                list = notes,
                onSettle = onReorder,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { index, note, isDragging ->
                val appearance = cardSettings[note.fileUri] ?: NoteCardAppearance()
                ReorderableItem {
                SwipeToDelete(
                    onDelete = { onDeleteRequested(note) },
                    hintOffset = if (index == 0) hintOffset.value else 0f
                ) {
                NoteCardSettingsRow(
                    note = note,
                    appearance = appearance,
                    expanded = expandedNoteUri == note.fileUri,
                    isDragging = isDragging,
                    dragHandleModifier = Modifier.draggableHandle(),
                    onToggleExpanded = { onToggleExpanded(note) },
                    onSizeSelected = { size -> onCardSizeSelected(note, size) },
                    onColorSelected = { color -> onCardColorSelected(note, color) },
                    onTextSizeSelected = { textSize -> onCardTextSizeSelected(note, textSize) },
                    onCustomColorChanged = { colorHex -> onCardCustomColorChanged(note, colorHex) }
                )
                }
                }
            }
        }
    }
}

@Composable
private fun NoteCardSettingsRow(
    note: NoteSummary,
    appearance: NoteCardAppearance,
    expanded: Boolean,
    isDragging: Boolean,
    dragHandleModifier: Modifier,
    onToggleExpanded: () -> Unit,
    onSizeSelected: (NoteCardSize) -> Unit,
    onColorSelected: (NoteCardColor) -> Unit,
    onTextSizeSelected: (NoteCardTextSize) -> Unit,
    onCustomColorChanged: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        // Lift the card while it is being dragged so it reads as picked up.
        tonalElevation = if (isDragging) 8.dp else 0.dp,
        shadowElevation = if (isDragging) 8.dp else 0.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(cardColorPreview(appearance.color), RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onToggleExpanded)
                )
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = stringResource(R.string.card_drag_hint),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = dragHandleModifier.padding(8.dp)
                )
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (expanded) stringResource(R.string.card_collapse) else stringResource(R.string.card_expand)
                    )
                }
            }

            if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                NoteCardSize.entries.forEach { size ->
                    FilterChip(
                        selected = appearance.size == size,
                        onClick = { onSizeSelected(size) },
                        label = { Text(size.label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.card_text_size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                NoteCardTextSize.entries.forEach { textSize ->
                    FilterChip(
                        selected = appearance.textSize == textSize,
                        onClick = { onTextSizeSelected(textSize) },
                        label = { Text(textSize.label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.card_color),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                NoteCardColor.entries.forEach { color ->
                    FilterChip(
                        selected = appearance.color == color,
                        onClick = { onColorSelected(color) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(cardColorPreview(color), RoundedCornerShape(3.dp))
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(color.label)
                            }
                        }
                    )
                }
            }
            if (appearance.color == NoteCardColor.Custom) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = appearance.customColorHex.orEmpty(),
                    onValueChange = onCustomColorChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.card_custom_color)) },
                    placeholder = { Text("#D9E8C8") },
                    singleLine = true
                )
            }
            }
        }
    }
}

private fun cardColorPreview(color: NoteCardColor): Color =
    NoteCardPalette.background(color) ?: Color(0xFFE7E0EC)

private const val KEY_SWIPE_HINT_SHOWN = "card_swipe_hint_shown"

/**
 * Prejeti prstem po karte na kteroukoli stranu vyvola smazani. Potvrzeni resi dialog vys,
 * takze se karta vzdy vrati zpatky — nic nemizi pod rukou.
 *
 * [hintOffset] je jen posun pro uvodni napovedu; gesto samo bezi pres stav dismiss boxu.
 */
@Composable
private fun SwipeToDelete(
    onDelete: () -> Unit,
    hintOffset: Float,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) onDelete()
            false
        }
    )
    Box(modifier = Modifier.offset { IntOffset(hintOffset.toInt(), 0) }) {
        SwipeToDismissBox(
            state = state,
            backgroundContent = { DeleteSwipeBackground(state.dismissDirection) },
            content = { content() }
        )
    }
}

@Composable
private fun DeleteSwipeBackground(direction: SwipeToDismissBoxValue) {
    val alignment = if (direction == SwipeToDismissBoxValue.EndToStart) {
        Alignment.CenterEnd
    } else {
        Alignment.CenterStart
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.card_delete_file),
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

/** Dva kroky, ktere musi kazdy projit. Vic jich neni — zbytek obrazovky je ladeni. */
internal enum class GuideStep { PickFolder, Save }

/**
 * Pomalu pulzujici ramecek kolem tlacitka, ktere je na rade. Bez prekryvu pres obrazovku:
 * uzivatel muze delat cokoli jineho, jen vidi, kudy vede cesta.
 */
@Composable
private fun GuideBanner(step: GuideStep, onSkip: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(
                    R.string.guide_step_counter,
                    if (step == GuideStep.PickFolder) 1 else 2
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            GuideLine(stringResource(R.string.guide_step_folder), done = step != GuideStep.PickFolder)
            GuideLine(stringResource(R.string.guide_step_widget), done = false)
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.action_skip))
            }
        }
    }
}

@Composable
private fun GuideLine(text: String, done: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Logo appky jako vodoznak. Lita krizem po obrazovce a odrazi se od okraju — vodorovne a svisle
 * ma jinou periodu, takze drahy se nikdy neopakuji uplne stejne.
 */
@Composable
private fun BoxScope.DriftingLogo(containerWidth: Dp, containerHeight: Dp) {
    val logoSize = 260.dp
    val travelX = (containerWidth - logoSize).coerceAtLeast(0.dp)
    val travelY = (containerHeight - logoSize).coerceAtLeast(0.dp)
    val transition = rememberInfiniteTransition(label = "logoDrift")
    val progressX by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "logoDriftX"
    )
    val progressY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(13_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "logoDriftY"
    )
    // Logo je svetle, takze na svetlem pozadi by bylo bila na bile — tam se prebarvi na tmavou
    // navy z ikony. Na tmavem pozadi se necha, jak je.
    //
    // Tma se pozna z pouziteho schematu, ne ze systemoveho nastaveni: uzivatel si v appce muze
    // vybrat opak toho, co ma v telefonu, a logo by pak splynulo s pozadim.
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Image(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = null,
        colorFilter = if (dark) null else ColorFilter.tint(androidx.compose.ui.graphics.Color(0xFF061B2A)),
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = travelX * progressX, y = travelY * progressY)
            .size(logoSize)
            .alpha(if (dark) 0.14f else 0.10f)
    )
}

private const val REPO_URL = "https://github.com/marfolog/notes-widget-for-markdown"
private const val ISSUES_URL = "$REPO_URL/issues/new/choose"
private const val DISCUSSIONS_URL = "$REPO_URL/discussions"
private const val PRIVACY_URL = "https://folmbuild.cz/notes-widget-privacy/"

/**
 * Odkazy ven. Otevirat je muze prohlizec, takze to funguje i ve foss variante, ktera sama
 * opravneni k internetu nema.
 */
@Composable
private fun AboutSection() {
    val context = LocalContext.current
    fun open(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(context, R.string.about_no_browser, Toast.LENGTH_SHORT).show()
        }
    }

    Text(
        text = stringResource(R.string.about_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.about_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(onClick = { open(ISSUES_URL) }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.about_report))
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = { open(DISCUSSIONS_URL) }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.about_say_hi))
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = { open(REPO_URL) }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.about_source))
    }
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = { open(PRIVACY_URL) }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.about_privacy))
    }
}

/** Poradi je zamerne: git je nejpresnejsi, "nic" je posledni moznost. */
private val SYNC_MODES = listOf(
    "git" to R.string.sync_mode_git,
    "other" to R.string.sync_mode_other,
    "none" to R.string.sync_mode_none
)

private val STALE_CHOICES = listOf(
    6 to R.string.stale_6h,
    24 to R.string.stale_1d,
    72 to R.string.stale_3d,
    168 to R.string.stale_1w
)
