package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.data.preferences.NoteCardSettingsStore
import com.marfolog.noteswidgetformarkdown.domain.model.FastNoteStyle
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.usecase.AppendFastNoteUseCase
import com.marfolog.noteswidgetformarkdown.domain.usecase.GetNotesUseCase
import com.marfolog.noteswidgetformarkdown.presentation.consent.ReportingConsentDialog
import com.marfolog.noteswidgetformarkdown.presentation.setup.FastNoteStyleChooser
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.ui.theme.NotesWidgetForMarkdownTheme
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.util.AfterNoteChoice
import com.marfolog.noteswidgetformarkdown.util.AfterNotePrompt
import com.marfolog.noteswidgetformarkdown.util.RatingPrompt
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get

class FastNoteActivity : ComponentActivity() {

    /** Off once the line is saved, so the rating dialog is not drawn over a stale form. */
    private var dialogVisible by mutableStateOf(true)
    private var askForRating by mutableStateOf(false)
    private var askForConsent by mutableStateOf(false)
    private lateinit var afterNote: AfterNoteAsk
    private var noteSaved = false
    private var targetUri by mutableStateOf<String?>(null)
    private var targetName by mutableStateOf<String?>(null)
    private var targetExists by mutableStateOf<Boolean?>(null)
    private var folderNotes by mutableStateOf<Result<List<NoteSummary>>?>(null)
    private var saving by mutableStateOf(false)
    private var style by mutableStateOf(FastNoteStyle.Bullet)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Turned or recreated after the line was saved, with a dialog on screen: show that dialog
        // again, never the empty form, which would offer to save the same line a second time.
        val restoredAfterSave = savedInstanceState?.getBoolean(STATE_NOTE_SAVED) == true
        if (restoredAfterSave) {
            noteSaved = true
            dialogVisible = false
            askForRating = savedInstanceState?.getBoolean(STATE_ASK_RATING) == true
            askForConsent = savedInstanceState?.getBoolean(STATE_ASK_CONSENT) == true
        } else {
            afterNote = AfterNoteAsk(this, lifecycleScope, UsageEvent.FROM_AFTER_FAST_NOTE)
        }

        val prefs = getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
        val notesFolderUri = prefs.getString(SetupActivity.KEY_NOTES_URI, null)
        targetUri = prefs.getString(SetupActivity.KEY_FAST_NOTE_TARGET_URI, null)
        style = FastNoteStyle.from(prefs.getString(SetupActivity.KEY_FAST_NOTE_STYLE, null))
        lifecycleScope.launch {
            val target = targetUri
            if (!target.isNullOrBlank()) {
                val file = withContext(Dispatchers.IO) { usableTarget(target) }
                targetName = file?.name
                targetExists = file != null
            }
            if (target.isNullOrBlank() || targetExists == false) {
                if (!notesFolderUri.isNullOrBlank()) folderNotes = loadFolderNotes(notesFolderUri)
            }
        }

        setContent {
            NotesWidgetForMarkdownTheme {
                if (askForRating) {
                    AfterNoteRatingHost(
                        from = UsageEvent.FROM_AFTER_FAST_NOTE,
                        onClose = { choice ->
                            if (choice == AfterNoteChoice.Rate) RatingPrompt.openRatePage(this)
                            finish()
                        }
                    )
                }
                if (askForConsent) {
                    ReportingConsentDialog(onAnswered = { finish() })
                }
                if (dialogVisible) {
                    FastNoteWindow(onDismissRequest = { if (!saving) finish() }) {
                        when (val step = fastNoteStep(targetUri, targetExists, notesFolderUri, folderNotes)) {
                            FastNoteStep.Write -> WriteContent(
                                targetName = targetName,
                                isSaving = saving,
                                style = style,
                                onStyleChange = ::changeStyle,
                                onCancel = { finish() },
                                onSave = ::save
                            )
                            FastNoteStep.Loading -> LoadingContent(onCancel = { finish() })
                            is FastNoteStep.PickTarget -> PickTargetContent(
                                notes = step.notes,
                                onPick = ::pickTarget,
                                onCancel = { finish() }
                            )
                            FastNoteStep.NoNotesFolder -> NoTargetContent(
                                messageRes = R.string.fast_note_no_folder,
                                onOpenSettings = ::openSettings,
                                onCancel = { finish() }
                            )
                            FastNoteStep.NoNotesInFolder -> NoTargetContent(
                                messageRes = R.string.fast_note_folder_empty,
                                onOpenSettings = ::openSettings,
                                onCancel = { finish() }
                            )
                            FastNoteStep.NotesUnreadable -> NoTargetContent(
                                messageRes = R.string.notes_load_failed,
                                onOpenSettings = ::openSettings,
                                onCancel = { finish() }
                            )
                        }
                    }
                }
            }
        }

        // Restored after the save with nothing left to show, such as a review card that was on its way.
        if (restoredAfterSave && !askForRating && !askForConsent) finish()
    }

    private fun usableTarget(target: String): DocumentFile? = runCatching {
        DocumentFile.fromSingleUri(this, Uri.parse(target))
            ?.takeIf { it.exists() && it.canRead() && it.canWrite() }
    }.getOrNull()

    /** Same notes, in the same order, as the target chips in settings. */
    private suspend fun loadFolderNotes(notesFolderUri: String): Result<List<NoteSummary>> = runCatching {
        val getNotesUseCase = get<GetNotesUseCase>(GetNotesUseCase::class.java)
        NoteCardSettingsStore(applicationContext)
            .applyOrder(getNotesUseCase(notesFolderUri).firstOrNull().orEmpty())
    }.onFailure { AppLog.w(AREA, "Could not list notes to pick a fast note target", it) }

    private fun pickTarget(note: NoteSummary) {
        getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(SetupActivity.KEY_FAST_NOTE_TARGET_URI, note.fileUri)
            .apply()
        Telemetry.event(UsageEvent.FAST_TARGET_SET)
        targetName = note.fileName
        targetUri = note.fileUri
        targetExists = true
    }

    private fun openSettings() {
        startActivity(SetupActivity.intent(this, UsageEvent.FROM_FAST_NOTE_ERROR))
        finish()
    }

    private fun changeStyle(newStyle: FastNoteStyle) {
        if (newStyle == style) return
        style = newStyle
        getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(SetupActivity.KEY_FAST_NOTE_STYLE, newStyle.storageValue)
            .apply()
        Telemetry.event(
            UsageEvent.SETTING_CHANGED,
            UsageEvent.PARAM_SETTING to UsageEvent.SETTING_FAST_NOTE_STYLE,
            UsageEvent.PARAM_VALUE to newStyle.storageValue
        )
    }

    private fun save(noteText: String, onError: (String) -> Unit) {
        val target = targetUri
        if (target.isNullOrBlank()) {
            onError(getString(R.string.fast_note_pick_target_first))
            return
        }
        saving = true
        val style = style
        lifecycleScope.launch {
            val appendFastNoteUseCase = get<AppendFastNoteUseCase>(AppendFastNoteUseCase::class.java)
            appendFastNoteUseCase(target, noteText, style)
                .onSuccess { saved ->
                    if (saved) {
                        Telemetry.event(UsageEvent.FAST_NOTE_SAVED, UsageEvent.PARAM_STYLE to style.storageValue)
                        Toast.makeText(
                            this@FastNoteActivity,
                            getString(R.string.fast_note_saved),
                            Toast.LENGTH_SHORT
                        ).show()
                        // Use applicationContext: the widget render is async and outlives this
                        // Activity, which finish() destroys.
                        NotesWidget.updateAll(applicationContext)
                        // A note just saved is a fair moment to ask; most of the time it is not
                        // time yet and this closes as fast as it always did.
                        dialogVisible = false
                        noteSaved = true
                        afterSave()
                    } else {
                        saving = false
                        onError(getString(R.string.fast_note_write_failed))
                    }
                }
                .onFailure { error ->
                    saving = false
                    onError(error.message ?: getString(R.string.fast_note_failed))
                }
        }
    }

    /**
     * After a saved note a single thing may follow: the reporting question, Google's review card or
     * the app's own rating dialog. A failed save never gets here, the form stays up with its error.
     */
    private suspend fun afterSave() {
        when (afterNote.decide(saved = true)) {
            AfterNotePrompt.Consent -> askForConsent = true
            AfterNotePrompt.Rating -> askForRating = true
            AfterNotePrompt.NativeReview -> {
                afterNote.launchNativeReview()
                finish()
            }
            AfterNotePrompt.None -> finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_NOTE_SAVED, noteSaved)
        outState.putBoolean(STATE_ASK_RATING, askForRating)
        outState.putBoolean(STATE_ASK_CONSENT, askForConsent)
    }

    private companion object {
        const val AREA = "FastNote"
        const val STATE_NOTE_SAVED = "note_saved"
        const val STATE_ASK_RATING = "ask_rating"
        const val STATE_ASK_CONSENT = "ask_consent"
    }
}

/**
 * One dialog window for every step. Swapping separate dialogs moved window focus between windows
 * right as the keyboard came up, and on a busy device the focus change was not handled in time.
 */
@Composable
private fun FastNoteWindow(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            modifier = Modifier.sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun StepLayout(
    title: @Composable () -> Unit,
    body: @Composable () -> Unit,
    buttons: @Composable RowScope.() -> Unit
) {
    Column(modifier = Modifier.padding(24.dp)) {
        ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
            CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.titleContentColor) {
                title()
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
            CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.textContentColor) {
                body()
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            modifier = Modifier.fillMaxWidth(),
            content = buttons
        )
    }
}

@Composable
private fun LoadingContent(onCancel: () -> Unit) {
    StepLayout(
        title = { Text(stringResource(R.string.fast_note_pick_title)) },
        body = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        },
        buttons = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun PickTargetContent(
    notes: List<NoteSummary>,
    onPick: (NoteSummary) -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 24.dp)) {
        Text(
            text = stringResource(R.string.fast_note_pick_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.fast_note_pick_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState())
        ) {
            notes.forEach { note -> TargetRow(note.fileName, onClick = { onPick(note) }) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = 12.dp)
        ) {
            Text(stringResource(R.string.action_cancel))
        }
    }
}

@Composable
private fun TargetRow(fileName: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = fileName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NoTargetContent(messageRes: Int, onOpenSettings: () -> Unit, onCancel: () -> Unit) {
    StepLayout(
        title = { Text(stringResource(R.string.fast_note_target_missing)) },
        body = { Text(stringResource(messageRes)) },
        buttons = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.action_open_settings_button))
            }
        }
    )
}

@Composable
private fun WriteContent(
    targetName: String?,
    isSaving: Boolean,
    style: FastNoteStyle,
    onStyleChange: (FastNoteStyle) -> Unit,
    onCancel: () -> Unit,
    onSave: (String, (String) -> Unit) -> Unit
) {
    var noteText by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // The whole point of a fast note is typing straight away — opening the dialog and then
    // tapping the field first defeats it. Waiting a frame lets the field be laid out first.
    LaunchedEffect(focusRequester) {
        withFrameNanos { }
        focusRequester.requestFocus()
        keyboard?.show()
    }

    StepLayout(
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
            body = {
                Column {
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
                Spacer(modifier = Modifier.height(12.dp))
                FastNoteStyleChooser(selected = style, onSelected = onStyleChange)
            }
        },
        buttons = {
            TextButton(
                enabled = !isSaving,
                onClick = onCancel
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            TextButton(
                enabled = noteText.isNotBlank() && !isSaving,
                onClick = { onSave(noteText) { message -> errorMessage = message } }
            ) {
                Text(stringResource(if (isSaving) R.string.fast_note_saving else R.string.fast_note_add))
            }
        }
    )
}
