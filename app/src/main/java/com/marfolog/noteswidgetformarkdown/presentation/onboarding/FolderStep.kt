package com.marfolog.noteswidgetformarkdown.presentation.onboarding

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.domain.usecase.GetNotesUseCase
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.AppLog
import kotlinx.coroutines.flow.firstOrNull
import org.koin.java.KoinJavaComponent.get

/** The picked folder as the step shows it; [notesFound] is null while the notes are being counted. */
internal data class FolderSummary(val name: String, val notesFound: Int?)

@Composable
internal fun rememberFolderSummary(notesUri: String?): FolderSummary? {
    val context = LocalContext.current
    val name = remember(notesUri) { notesUri?.let { folderName(context, it) } }
    var notesFound by remember(notesUri) { mutableStateOf<Int?>(null) }
    LaunchedEffect(notesUri) {
        val uri = notesUri ?: return@LaunchedEffect
        notesFound = runCatching {
            get<GetNotesUseCase>(GetNotesUseCase::class.java)(uri).firstOrNull().orEmpty().size
        }.onFailure { AppLog.w(SetupActivity.AREA, "Could not count notes in the picked folder", it) }
            .getOrDefault(0)
    }
    return name?.let { FolderSummary(it, notesFound) }
}

@Composable
internal fun ColumnScope.FolderStep(
    folder: FolderSummary?,
    onPick: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    StepLayout(
        titleRes = R.string.onboarding_folder_title,
        bodyRes = R.string.onboarding_folder_body,
        content = {
            if (folder != null) {
                StatusCard(
                    icon = Icons.Outlined.Folder,
                    title = folder.name,
                    text = folder.notesFound?.let {
                        pluralStringResource(R.plurals.onboarding_folder_notes_found, it, it)
                    } ?: stringResource(R.string.onboarding_folder_counting)
                )
                if (Onboarding.warnsAboutEmptyFolder(folder.notesFound)) {
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusCard(
                        icon = Icons.Outlined.Info,
                        title = null,
                        text = stringResource(R.string.onboarding_folder_empty),
                        warning = true
                    )
                }
            }
        },
        buttons = {
            if (folder == null) {
                Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.onboarding_folder_pick))
                }
            } else {
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.onboarding_continue))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.onboarding_folder_change))
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                Text(stringResource(R.string.onboarding_back))
            }
        }
    )
}

/** The provider's name for the folder, or the last part of its path when it cannot say. */
private fun folderName(context: Context, uri: String): String =
    runCatching { DocumentFile.fromTreeUri(context, Uri.parse(uri))?.name }.getOrNull()
        ?: Uri.decode(uri.substringAfterLast('/')).substringAfterLast('/').substringAfterLast(':')
