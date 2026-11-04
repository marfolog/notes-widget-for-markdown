package com.marfolog.noteswidgetformarkdown.presentation.widget

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.ui.theme.NotesWidgetForMarkdownTheme
import com.marfolog.noteswidgetformarkdown.util.AfterNoteChoice
import com.marfolog.noteswidgetformarkdown.util.AfterNoteRating
import com.marfolog.noteswidgetformarkdown.util.RatingPrompt
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.UsageEvent

/**
 * The rating dialog that follows a saved note, with the choice recorded. [onClose] runs once nothing
 * of it is on screen any more, with the choice: opening the store page after a "Rate" is left to the
 * activity, which knows what else it has to open. [from] says which activity asked, in the analytics.
 */
@Composable
internal fun AfterNoteRatingHost(from: String, onClose: (AfterNoteChoice) -> Unit) {
    val context = LocalContext.current

    fun choose(choice: AfterNoteChoice) {
        AfterNoteRating.recordChoice(context, choice, System.currentTimeMillis())
        Telemetry.event(
            UsageEvent.RATE_DIALOG_CHOICE,
            UsageEvent.PARAM_CHOICE to choice.eventValue,
            UsageEvent.PARAM_FROM to from
        )
        onClose(choice)
    }

    AfterNoteRatingDialog(
        titleRes = RatingPrompt.AFTER_NOTE_TITLE_RES,
        bodyRes = RatingPrompt.RATE_BODY_RES,
        rateButtonRes = RatingPrompt.RATE_BUTTON_RES,
        onChoice = ::choose
    )
}

/** A statement, not a question: Google does not allow asking for an opinion before its own rating card. */
@Composable
private fun AfterNoteRatingDialog(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
    @StringRes rateButtonRes: Int,
    onChoice: (AfterNoteChoice) -> Unit
) {
    Dialog(onDismissRequest = { onChoice(AfterNoteChoice.Dismissed) }) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { onChoice(AfterNoteChoice.Rate) }, modifier = Modifier.fillMaxWidth()) {
                    OneLineLabel(stringResource(rateButtonRes))
                }
                TextButton(
                    onClick = { onChoice(AfterNoteChoice.Later) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    OneLineLabel(stringResource(R.string.after_note_rating_later))
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Rating after a saved note")
@Composable
private fun AfterNoteRatingDialogPreview() {
    NotesWidgetForMarkdownTheme {
        AfterNoteRatingDialog(
            titleRes = R.string.after_note_rating_title_play,
            bodyRes = R.string.rate_dialog_body_play,
            rateButtonRes = R.string.rate_dialog_rate_play,
            onChoice = {}
        )
    }
}
