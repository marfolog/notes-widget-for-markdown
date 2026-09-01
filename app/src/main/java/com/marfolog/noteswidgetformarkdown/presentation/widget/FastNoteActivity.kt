package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.marfolog.noteswidgetformarkdown.util.AppLog
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
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

        setContent {
            NotesWidgetForMarkdownTheme {
                FastNoteDialog(
                    hasTarget = !targetUri.isNullOrBlank(),
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
                            onError("Choose a Fast note target in settings first.")
                        } else {
                            lifecycleScope.launch {
                                val appendFastNoteUseCase =
                                    get<AppendFastNoteUseCase>(AppendFastNoteUseCase::class.java)
                                appendFastNoteUseCase(targetUri, noteText)
                                    .onSuccess { saved ->
                                        if (saved) {
                                            Toast.makeText(
                                                this@FastNoteActivity,
                                                "Fast note saved locally",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            // Use applicationContext: the widget
                                            // render is async and outlives this
                                            // Activity, which finish() destroys.
                                            NotesWidget.updateAll(applicationContext)
                                            finish()
                                        } else {
                                            onError("Unable to write to the selected note.")
                                        }
                                    }
                                    .onFailure { error ->
                                        onError(error.message ?: "Unable to save Fast note.")
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
            title = { Text("Fast note target missing") },
            text = { Text("Choose a Markdown file in settings before saving Fast notes.") },
            confirmButton = {
                TextButton(onClick = onOpenSettings) {
                    Text("Open settings")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
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
        title = { Text("Fast note") },
        text = {
            OutlinedTextField(
                value = noteText,
                onValueChange = {
                    noteText = it
                    errorMessage = null
                },
                label = { Text("Note") },
                supportingText = {
                    val message = errorMessage ?: "Saved locally. Sync remains handled by your notes setup."
                    Text(message)
                },
                isError = errorMessage != null,
                minLines = 3
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
                Text(if (isSaving) "Saving..." else "Add")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onCancel
            ) {
                Text("Cancel")
            }
        }
    )
}
