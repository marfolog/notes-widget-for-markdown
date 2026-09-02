package com.marfolog.noteswidgetformarkdown.presentation.setup

import android.app.Activity
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
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
import com.marfolog.noteswidgetformarkdown.presentation.widget.NotesWidget
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.worker.NotesFolderObserverJob
import com.marfolog.noteswidgetformarkdown.ui.theme.NoteCardPalette
import com.marfolog.noteswidgetformarkdown.ui.theme.NotesWidgetForMarkdownTheme
import sh.calvin.reorderable.ReorderableColumn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get

class SetupActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotesWidgetForMarkdownTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(title = { Text("Notes Widget Settings") })
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
            notesLoadError = error.message ?: "Unable to load notes"
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
    val notesDisplayName = notesUri?.let { uriString ->
        runCatching {
            DocumentFile.fromTreeUri(context, Uri.parse(uriString))?.name
        }.getOrNull()
    }

    val onSave: () -> Unit = {
        Toast.makeText(context, "Settings saved!", Toast.LENGTH_SHORT).show()

        scope.launch {
            NotesWidget.updateAll(context)
            (context as? Activity)?.finish()
        }
    }

    SetupScreenContent(
        vaultName = vaultDisplayName,
        onSelectVault = { vaultPicker.launch(null) },
        notesFolderName = notesDisplayName,
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
        gitStatusText = gitStatusText(gitStatus),
        gitIsTracked = gitStatus is GitSyncStatus.Tracked,
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
            title = { Text("Delete ${note.fileName}?") },
            text = { Text("The file will be permanently removed from your device. This cannot be undone.") },
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
                                        error.message ?: "Could not delete the file",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { noteToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

private fun gitStatusText(status: GitSyncStatus): String = when (status) {
    is GitSyncStatus.NotTracked ->
        "No .git folder found in the folders this app can access. If your vault is synced by " +
            "Obsidian Git or a similar client, select the folder that contains .git — usually " +
            "one level above your notes folder."
    is GitSyncStatus.Unavailable ->
        "Found .git but could not read it: ${status.reason}"
    is GitSyncStatus.Tracked -> buildString {
        // This is when HEAD last moved — a pull, a commit, a checkout. Not "when the note
        // was written", and not "when the client last ran".
        append("Last repo change")
        status.lastAction?.let { append(" ($it)") }
        append(": ")
        append(
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(status.lastChangeAtMillis))
        )
        status.branch?.let { append(" · branch $it") }
        status.lastFetchAtMillis?.let { fetchedAt ->
            append("\nLast contact with remote: ")
            append(
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(fetchedAt))
            )
        }
        // Two different questions: "when did anything arrive" vs "is the client even running".
        status.lastClientActivityAtMillis?.let { activeAt ->
            append("\nSync client last active: ")
            append(
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(activeAt))
            )
        }
        val problem = when (status.problem) {
            GitSyncStatus.Problem.None -> null
            GitSyncStatus.Problem.Conflict ->
                "Unfinished merge (MERGE_HEAD present) — most likely a conflict waiting in your git client."
            GitSyncStatus.Problem.RebaseInProgress ->
                "A rebase was started and never finished — resolve it in your git client."
            GitSyncStatus.Problem.Diverged ->
                "Local branch differs from origin — changes are waiting to be pushed or pulled."
        }
        problem?.let { append("\n\n⚠ $it") }
    }
}

@Composable
internal fun SetupScreenContent(
    vaultName: String?,
    onSelectVault: () -> Unit,
    notesFolderName: String?,
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
    onSelectGitFolder: () -> Unit = {},
    showGitStatus: Boolean = true,
    onShowGitStatusChanged: (Boolean) -> Unit = {},
    detectedVaultName: String? = null,
    telemetryAvailable: Boolean = false,
    telemetryEnabled: Boolean = false,
    onTelemetryChanged: (Boolean) -> Unit = {},
    canSave: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 24.dp)
                .size(280.dp)
                .alpha(0.055f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
        // Section 1: the only folder anyone has to pick
        Text(
            text = "Notes Folder",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "The folder the widget will read .md files from. Can be the root folder or a subfolder.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (notesFolderName != null) {
            Text(
                text = notesFolderName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(onClick = onSelectNotesFolder, modifier = Modifier.fillMaxWidth()) {
            Text(text = if (notesFolderName != null) "Change Notes Folder" else "Select Notes Folder")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Section 2: only needed when the vault could not be worked out
        if (notesFolderName == null) {
            // Nothing to say yet — everything below depends on the folder above.
        } else if (detectedVaultName != null) {
            Text(
                text = "Vault detected: $detectedVaultName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        } else {
        Text(
            text = "Vault root (optional)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Only needed if your notes folder is not the top of your vault. Without it, " +
                "tapping a note may open the wrong one, and a .git folder one level up stays " +
                "invisible.",
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

        OutlinedButton(onClick = onSelectVault, modifier = Modifier.fillMaxWidth()) {
            Text(text = if (vaultName != null) "Change Root Folder" else "Select Root Folder")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        }

        // Only the Play build has anything to switch off; the foss build never reports.
        if (telemetryAvailable) {
            Text(
                text = "Reporting",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Send anonymous usage and crash reports",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = telemetryEnabled, onCheckedChange = onTelemetryChanged)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Helps find crashes and shows how many people use the app. Never includes " +
                    "note contents, file names or folder paths. Switching this off stops all " +
                    "reporting immediately.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section 3: Git sync status — meaningless before a folder is picked
        if (notesFolderName != null) {
        Text(
            text = "Git Sync Status",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "The app never runs git itself. It only reads .git to show in the widget when " +
                "your vault was last pulled or committed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Show in widget",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = showGitStatus, onCheckedChange = onShowGitStatusChanged)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Turn this off if you sync by other means — Syncthing, Obsidian Sync, " +
                "or manual copying. The widget then shows no sync chip at all.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = gitStatusText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (gitIsTracked) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.error
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onSelectGitFolder, modifier = Modifier.fillMaxWidth()) {
            Text(text = if (gitIsTracked) "Change Git Folder" else "Select Folder Containing .git")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        }

        // Section 4: Derived info
        Text(
            text = "New Notes Path",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        val vaultLabel = detectedVaultName ?: vaultName
        if (notesFolderName != null) {
            val displayPath = when {
                vaultLabel == null -> notesFolderName
                derivedPath.isEmpty() -> "$vaultLabel/ (root folder)"
                else -> "$vaultLabel/$derivedPath"
            }
            Text(
                text = "New notes will be created in: $displayPath",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Pick a notes folder to see where new notes will go.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Fast Note Target",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Fast notes are saved locally into this Markdown file. Sync remains handled by your notes setup.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        FastNoteTargetSection(
            notes = notes,
            notesLoadError = notesLoadError,
            selectedNoteUri = fastNoteTargetUri,
            onTargetSelected = onFastNoteTargetSelected
        )

        Spacer(modifier = Modifier.height(32.dp))

        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Card Appearance",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Set height and color for each note card in the widget.",
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

        // Save button
        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save & Apply")
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
    onTargetSelected: (NoteSummary) -> Unit
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
            Text(
                text = "Select a notes folder with .md files to choose a Fast note target.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            val selectedExists = selectedNoteUri == null || notes.any { it.fileUri == selectedNoteUri }
            if (!selectedExists) {
                Text(
                    text = "The selected Fast note target is no longer available. Choose another note.",
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
                text = "Select a notes folder with .md files to configure cards.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            ReorderableColumn(
                list = notes,
                onSettle = onReorder,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { _, note, isDragging ->
                val appearance = cardSettings[note.fileUri] ?: NoteCardAppearance()
                ReorderableItem {
                NoteCardSettingsRow(
                    note = note,
                    appearance = appearance,
                    expanded = expandedNoteUri == note.fileUri,
                    isDragging = isDragging,
                    dragHandleModifier = Modifier.draggableHandle(),
                    onToggleExpanded = { onToggleExpanded(note) },
                    onDeleteRequested = { onDeleteRequested(note) },
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

@Composable
private fun NoteCardSettingsRow(
    note: NoteSummary,
    appearance: NoteCardAppearance,
    expanded: Boolean,
    isDragging: Boolean,
    dragHandleModifier: Modifier,
    onToggleExpanded: () -> Unit,
    onDeleteRequested: () -> Unit,
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
                IconButton(onClick = onDeleteRequested) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete note file",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
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
                        contentDescription = if (expanded) "Collapse" else "Expand appearance"
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
                text = "Text size",
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
                text = "Color",
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
                    label = { Text("Custom color") },
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
