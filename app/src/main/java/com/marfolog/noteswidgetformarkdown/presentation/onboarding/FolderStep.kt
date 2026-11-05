package com.marfolog.noteswidgetformarkdown.presentation.onboarding

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.domain.model.FolderDiagnosis
import com.marfolog.noteswidgetformarkdown.domain.model.SubfolderCount
import com.marfolog.noteswidgetformarkdown.presentation.setup.SubfolderPickerDialog
import com.marfolog.noteswidgetformarkdown.presentation.setup.folderProblemText
import com.marfolog.noteswidgetformarkdown.presentation.setup.rememberFolderDiagnosis

/** The picked folder as the step shows it; [diagnosis] is null while the folder is being looked at. */
internal data class FolderSummary(val name: String, val diagnosis: FolderDiagnosis?)

@Composable
internal fun rememberFolderSummary(notesUri: String?): FolderSummary? {
    val context = LocalContext.current
    val name = remember(notesUri) { notesUri?.let { folderName(context, it) } }
    val diagnosis = rememberFolderDiagnosis(notesUri)
    return name?.let { FolderSummary(it, diagnosis) }
}

@Composable
internal fun ColumnScope.FolderStep(
    folder: FolderSummary?,
    onPick: () -> Unit,
    onPickSubfolder: (SubfolderCount) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var showSubfolders by rememberSaveable { mutableStateOf(false) }
    val diagnosis = folder?.diagnosis
    val primary = Onboarding.folderPrimary(diagnosis)

    StepLayout(
        titleRes = R.string.onboarding_folder_title,
        bodyRes = R.string.onboarding_folder_body,
        content = {
            if (folder != null) {
                val problem = diagnosis?.let { folderProblemText(folder.name, it, R.string.onboarding_folder_empty) }
                StatusCard(
                    icon = Icons.Outlined.Folder,
                    title = folder.name,
                    text = problem ?: when (diagnosis) {
                        is FolderDiagnosis.HasNotes ->
                            pluralStringResource(R.plurals.onboarding_folder_notes_found, diagnosis.count, diagnosis.count)
                        else -> stringResource(R.string.onboarding_folder_counting)
                    },
                    warning = problem != null
                )
            }
        },
        buttons = {
            if (folder == null) {
                Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.onboarding_folder_pick))
                }
            } else {
                when (primary) {
                    FolderPrimary.Continue -> {
                        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.onboarding_continue))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.onboarding_folder_change))
                        }
                    }
                    FolderPrimary.PickSubfolder -> {
                        Button(onClick = { showSubfolders = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.pick_subfolder))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.onboarding_folder_change))
                        }
                        TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.onboarding_continue))
                        }
                    }
                    FolderPrimary.ChooseFolder -> {
                        Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.onboarding_folder_change))
                        }
                        TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.onboarding_continue))
                        }
                    }
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                Text(stringResource(R.string.onboarding_back))
            }
        }
    )

    if (showSubfolders && diagnosis is FolderDiagnosis.NotesInSubfolders) {
        SubfolderPickerDialog(
            diagnosis = diagnosis,
            onPick = { subfolder ->
                showSubfolders = false
                onPickSubfolder(subfolder)
            },
            onDismiss = { showSubfolders = false }
        )
    }
}

/** The provider's name for the folder, or the last part of its path when it cannot say. */
private fun folderName(context: Context, uri: String): String =
    runCatching { DocumentFile.fromTreeUri(context, Uri.parse(uri))?.name }.getOrNull()
        ?: Uri.decode(uri.substringAfterLast('/')).substringAfterLast('/').substringAfterLast(':')
