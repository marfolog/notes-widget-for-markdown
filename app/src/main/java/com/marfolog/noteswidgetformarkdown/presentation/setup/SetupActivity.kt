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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.marfolog.noteswidgetformarkdown.presentation.widget.NotesWidget
import com.marfolog.noteswidgetformarkdown.ui.theme.NotesWidgetForMarkdownTheme
import kotlinx.coroutines.launch

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
    }
}

@Composable
private fun SetupScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()

    var vaultUri by rememberSaveable { mutableStateOf(prefs.getString(SetupActivity.KEY_VAULT_URI, null)) }
    var notesUri by rememberSaveable { mutableStateOf(prefs.getString(SetupActivity.KEY_NOTES_URI, null)) }
    var vaultName by rememberSaveable { mutableStateOf(prefs.getString(SetupActivity.KEY_VAULT_NAME, null)) }
    var derivedPath by rememberSaveable { mutableStateOf(prefs.getString(SetupActivity.KEY_NOTE_FOLDER_PATH, null) ?: "") }

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
