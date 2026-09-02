package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.util.AppLog
import androidx.activity.compose.setContent
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.remember
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.marfolog.noteswidgetformarkdown.domain.usecase.AppendFastNoteUseCase
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.ui.theme.NotesWidgetForMarkdownTheme
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get

class FastNoteActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
        val targetUri = prefs.getString(SetupActivity.KEY_FAST_NOTE_TARGET_URI, null)
        // Show where the line will land. The target is set once in settings and then forgotten,
        // so without this the dialog asks you to type into an unknown file.
        val targetName = targetUri
            ?.let { runCatching { DocumentFile.fromSingleUri(this, Uri.parse(it))?.name }.getOrNull() }

        setContent {
            NotesWidgetForMarkdownTheme {
                FastNoteDialog(
                    hasTarget = !targetUri.isNullOrBlank(),
                    targetName = targetName,
                    onCancel = { finish() },
                    onOpenSettings = {
                        startActivity(
                            Intent(this, SetupActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                        )
                        finish()
                    },
                    onSave = { noteText, onError ->
                        if (targetUri.isNullOrBlank()) {
                            onError(getString(R.string.fast_note_pick_target_first))
                        } else {
                            lifecycleScope.launch {
                                val appendFastNoteUseCase =
                                    get<AppendFastNoteUseCase>(AppendFastNoteUseCase::class.java)
                                appendFastNoteUseCase(targetUri, noteText)
                                    .onSuccess { saved ->
                                        if (saved) {
                                            Toast.makeText(
                                                this@FastNoteActivity,
                                                getString(R.string.fast_note_saved),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            // Use applicationContext: the widget
                                            // render is async and outlives this
                                            // Activity, which finish() destroys.
                                            NotesWidget.updateAll(applicationContext)
                                            finish()
                                        } else {
                                            onError(getString(R.string.fast_note_write_failed))
                                        }
                                    }
                                    .onFailure { error ->
                                        onError(error.message ?: getString(R.string.fast_note_failed))
                                    }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FastNoteDialog(
    hasTarget: Boolean,
    targetName: String? = null,
    onCancel: () -> Unit,
    onOpenSettings: () -> Unit,
    onSave: (String, (String) -> Unit) -> Unit
) {
    var noteText by rememberSaveable { mutableStateOf("") }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    if (!hasTarget) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = { Text(stringResource(R.string.fast_note_target_missing)) },
            text = { Text(stringResource(R.string.fast_note_target_hint)) },
            confirmButton = {
                TextButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.action_open_settings_button))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                onCancel()
            }
        },
        title = {
            Column {
                Text(stringResource(R.string.fast_note_title))
                targetName?.let { name ->
                    Text(
                        text = stringResource(R.string.fast_note_adds_line_to, name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            val focusRequester = remember { FocusRequester() }
            val keyboard = LocalSoftwareKeyboardController.current

            // The whole point of a fast note is typing straight away — opening the dialog and then
            // tapping the field first defeats it.
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
                keyboard?.show()
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = {
                    noteText = it
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.fast_note_field_label)) },
                // Only speak up when something went wrong. The old hint explained the sync model
                // to someone who just wants to jot a line down.
                supportingText = errorMessage?.let { message -> { Text(message) } },
                isError = errorMessage != null,
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                enabled = noteText.isNotBlank() && !isSaving,
                onClick = {
                    isSaving = true
                    onSave(noteText) { message ->
                        errorMessage = message
                        isSaving = false
                    }
                }
            ) {
                Text(stringResource(if (isSaving) R.string.fast_note_saving else R.string.fast_note_add))
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onCancel
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
