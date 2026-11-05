package com.marfolog.noteswidgetformarkdown.presentation.setup

import android.content.ActivityNotFoundException
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.data.repository.FolderDiagnoser
import com.marfolog.noteswidgetformarkdown.domain.model.FolderDiagnosis
import com.marfolog.noteswidgetformarkdown.domain.model.SubfolderCount
import com.marfolog.noteswidgetformarkdown.domain.model.summary
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.util.FeatureInterest
import com.marfolog.noteswidgetformarkdown.util.UsageEvent

/**
 * What is inside the picked folder, null while it is being looked at. Asks again when [notesUri]
 * or [reloadKey] changes; the answer is shared with every other asker for a few seconds.
 */
@Composable
internal fun rememberFolderDiagnosis(notesUri: String?, reloadKey: Any? = null, refresh: Boolean = false): FolderDiagnosis? {
    val context = LocalContext.current
    var diagnosis by remember(notesUri, reloadKey) { mutableStateOf<FolderDiagnosis?>(null) }
    LaunchedEffect(notesUri, reloadKey) {
        if (notesUri != null) diagnosis = FolderDiagnoser(context).diagnose(notesUri, refresh)
    }
    return diagnosis
}

/** The sentence that explains a folder with no notes of its own; null when there is nothing to explain. */
@Composable
internal fun folderProblemText(folderName: String, diagnosis: FolderDiagnosis, emptyRes: Int): String? =
    when (diagnosis) {
        is FolderDiagnosis.HasNotes -> null
        is FolderDiagnosis.NotesInSubfolders ->
            stringResource(R.string.folder_in_subfolders, folderName, diagnosis.summary())
        is FolderDiagnosis.NoMarkdown -> stringResource(R.string.folder_no_markdown)
        FolderDiagnosis.EmptyFolder -> stringResource(emptyRes)
        FolderDiagnosis.NoAccess -> stringResource(R.string.folder_no_access)
    }

/**
 * Where the system picker should open to make [subfolder] the notes folder. A grant on the folder
 * above does not reach a folder of its own, so the user confirms it once in the picker.
 */
internal fun subfolderPickerStart(notesUri: String?, subfolder: SubfolderCount): Uri? = runCatching {
    DocumentsContract.buildDocumentUri(Uri.parse(notesUri).authority, subfolder.documentId)
}.getOrNull()

/** A device without a folder picker (or one that is busy) must not take the app down with it. */
internal fun ActivityResultLauncher<Uri?>.launchSafely(startAt: Uri?) {
    try {
        launch(startAt)
    } catch (e: ActivityNotFoundException) {
        AppLog.w(SetupActivity.AREA, "No folder picker on this device", e)
    } catch (e: SecurityException) {
        AppLog.w(SetupActivity.AREA, "The folder picker could not be opened", e)
    }
}

private enum class InterestRow { Ask, Thanks, Hidden }

/**
 * The folders that hold notes, with how many each, busiest first. Nothing is preselected: tapping a
 * row is the choice. Underneath, once, the question whether a widget per folder would be used.
 */
@Composable
internal fun SubfolderPickerDialog(
    diagnosis: FolderDiagnosis.NotesInSubfolders,
    onPick: (SubfolderCount) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var interest by rememberSaveable {
        mutableStateOf(
            if (FeatureInterest.shouldShowListRow(
                    FeatureInterest.isAnswered(context, UsageEvent.FEATURE_MULTI_FOLDER_WIDGETS),
                    diagnosis.subfolders.size
                )
            ) InterestRow.Ask else InterestRow.Hidden
        )
    }
    // Saved, so turning the screen does not count the same showing twice.
    var reported by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (interest == InterestRow.Ask && !reported) {
            reported = true
            FeatureInterest.reportShown(UsageEvent.FEATURE_MULTI_FOLDER_WIDGETS, UsageEvent.FROM_SUBFOLDER_LIST)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_subfolder_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.pick_subfolder_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    diagnosis.listed.forEach { folder ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable(role = Role.Button) { onPick(folder) }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = folder.path,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = folder.count.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (interest != InterestRow.Hidden) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    when (interest) {
                        InterestRow.Thanks -> Text(
                            text = stringResource(R.string.feature_interest_thanks),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        else -> {
                            Text(
                                text = stringResource(R.string.feature_interest_text),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    FeatureInterest.answer(
                                        context, UsageEvent.FEATURE_MULTI_FOLDER_WIDGETS,
                                        UsageEvent.ANSWER_YES, UsageEvent.FROM_SUBFOLDER_LIST
                                    )
                                    interest = InterestRow.Thanks
                                }) { Text(stringResource(R.string.feature_interest_yes), maxLines = 1) }
                                TextButton(onClick = {
                                    FeatureInterest.answer(
                                        context, UsageEvent.FEATURE_MULTI_FOLDER_WIDGETS,
                                        UsageEvent.ANSWER_NO, UsageEvent.FROM_SUBFOLDER_LIST
                                    )
                                    interest = InterestRow.Hidden
                                }) { Text(stringResource(R.string.feature_interest_no), maxLines = 1) }
                            }
                            Text(
                                text = stringResource(R.string.feature_interest_anonymous),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
