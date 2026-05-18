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
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.marfolog.noteswidgetformarkdown.data.preferences.NoteCardSettingsStore
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardAppearance
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardColor
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardSize
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardTextSize
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.usecase.GetNotesUseCase
import com.marfolog.noteswidgetformarkdown.presentation.widget.NotesWidget
import com.marfolog.noteswidgetformarkdown.ui.theme.NotesWidgetForMarkdownTheme
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
        const val PREFS_NAME = "app_prefs"
        const val KEY_VAULT_URI = "vault_uri"
        const val KEY_NOTES_URI = "notes_uri"
        const val KEY_VAULT_NAME = "vault_name"
        const val KEY_NOTE_FOLDER_PATH = "note_folder_path"
        const val KEY_SYNC_URI = "sync_uri"
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
    var syncUri by rememberSaveable { mutableStateOf(prefs.getString(SetupActivity.KEY_SYNC_URI, null).orEmpty()) }
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
            notesForSettings = emptyList()
            notesLoadError = error.message ?: "Unable to load notes"
        }
    }

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
        prefs.edit()
            .putString(SetupActivity.KEY_SYNC_URI, syncUri.trim())
            .apply()

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
        syncUri = syncUri,
        onSyncUriChange = { syncUri = it },
        notes = notesForSettings,
        cardSettings = cardSettings,
        notesLoadError = notesLoadError,
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
        onMoveCard = { note, direction ->
            val currentIndex = notesForSettings.indexOfFirst { it.fileUri == note.fileUri }
            val targetIndex = currentIndex + direction
            if (currentIndex >= 0 && targetIndex in notesForSettings.indices) {
                notesForSettings = notesForSettings.toMutableList().also { notes ->
                    val moved = notes.removeAt(currentIndex)
                    notes.add(targetIndex, moved)
                }
                cardSettingsStore.saveOrder(notesForSettings.map { it.fileUri })
                scope.launch { NotesWidget.updateAll(context) }
            }
        },
        canSave = vaultUri != null && notesUri != null,
        onSave = onSave,
        modifier = modifier
    )
}

@Composable
internal fun SetupScreenContent(
    vaultName: String?,
    onSelectVault: () -> Unit,
    notesFolderName: String?,
    onSelectNotesFolder: () -> Unit,
    derivedPath: String,
    syncUri: String = "",
    onSyncUriChange: (String) -> Unit = {},
    notes: List<NoteSummary> = emptyList(),
    cardSettings: Map<String, NoteCardAppearance> = emptyMap(),
    notesLoadError: String? = null,
    onCardSizeSelected: (NoteSummary, NoteCardSize) -> Unit = { _, _ -> },
    onCardColorSelected: (NoteSummary, NoteCardColor) -> Unit = { _, _ -> },
    onCardTextSizeSelected: (NoteSummary, NoteCardTextSize) -> Unit = { _, _ -> },
    onCardCustomColorChanged: (NoteSummary, String) -> Unit = { _, _ -> },
    onMoveCard: (NoteSummary, Int) -> Unit = { _, _ -> },
    canSave: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Section 1: Obsidian Vault
        Text(
            text = "Obsidian Vault",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Select the root folder of your Obsidian vault.",
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
            Text(text = if (vaultName != null) "Change Vault Root" else "Select Vault Root")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Section 2: Notes Folder
        Text(
            text = "Notes Folder",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "The folder the widget will read .md files from. Can be the vault root or a subfolder.",
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

        // Section 3: Derived info
        Text(
            text = "New Notes Path",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (vaultName != null && notesFolderName != null) {
            val displayPath = if (derivedPath.isEmpty()) {
                "$vaultName/ (vault root)"
            } else {
                "$vaultName/$derivedPath"
            }
            Text(
                text = "New notes will be created in: $displayPath",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Select both folders to see the derived path.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Obsidian Sync",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Paste an Obsidian Advanced URI command for your Git pull or sync action.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = syncUri,
            onValueChange = onSyncUriChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Sync URI") },
            placeholder = { Text("obsidian://advanced-uri?...") },
            minLines = 2,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(24.dp))
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
            onMoveCard = onMoveCard
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

@Composable
private fun CardSettingsSection(
    notes: List<NoteSummary>,
    cardSettings: Map<String, NoteCardAppearance>,
    notesLoadError: String?,
    onCardSizeSelected: (NoteSummary, NoteCardSize) -> Unit,
    onCardColorSelected: (NoteSummary, NoteCardColor) -> Unit,
    onCardTextSizeSelected: (NoteSummary, NoteCardTextSize) -> Unit,
    onCardCustomColorChanged: (NoteSummary, String) -> Unit,
    onMoveCard: (NoteSummary, Int) -> Unit
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                notes.forEachIndexed { index, note ->
                    val appearance = cardSettings[note.fileUri] ?: NoteCardAppearance()
                    NoteCardSettingsRow(
                        note = note,
                        appearance = appearance,
                        canMoveUp = index > 0,
                        canMoveDown = index < notes.lastIndex,
                        onMoveUp = { onMoveCard(note, -1) },
                        onMoveDown = { onMoveCard(note, 1) },
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
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSizeSelected: (NoteCardSize) -> Unit,
    onColorSelected: (NoteCardColor) -> Unit,
    onTextSizeSelected: (NoteCardTextSize) -> Unit,
    onCustomColorChanged: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up"
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move down"
                    )
                }
            }
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

private fun cardColorPreview(color: NoteCardColor): Color {
    return when (color) {
        NoteCardColor.Default -> Color(0xFFE7E0EC)
        NoteCardColor.Rose -> Color(0xFFFFDAD6)
        NoteCardColor.Red -> Color(0xFFFFB4AB)
        NoteCardColor.Amber -> Color(0xFFFFDEA6)
        NoteCardColor.Orange -> Color(0xFFFFDCC2)
        NoteCardColor.Mint -> Color(0xFFBCECCB)
        NoteCardColor.Green -> Color(0xFFCDEDA3)
        NoteCardColor.Sky -> Color(0xFFC9E6FF)
        NoteCardColor.Blue -> Color(0xFFD0E4FF)
        NoteCardColor.Lavender -> Color(0xFFE7DEFF)
        NoteCardColor.Custom -> parseColorOrDefault(color = null, fallback = Color(0xFFE7E0EC))
    }
}

private fun parseColorOrDefault(color: String?, fallback: Color): Color {
    val normalized = color?.trim().orEmpty()
    if (!Regex("^#?[0-9A-Fa-f]{6}$").matches(normalized)) {
        return fallback
    }
    val hex = normalized.removePrefix("#")
    return Color(("FF$hex").toLong(16))
}
