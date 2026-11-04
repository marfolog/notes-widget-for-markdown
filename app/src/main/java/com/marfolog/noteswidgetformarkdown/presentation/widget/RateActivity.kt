package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.ui.theme.NotesWidgetForMarkdownTheme
import com.marfolog.noteswidgetformarkdown.util.AfterNoteRating
import com.marfolog.noteswidgetformarkdown.util.ProjectLinks
import com.marfolog.noteswidgetformarkdown.util.RateReminder
import com.marfolog.noteswidgetformarkdown.util.RatingPrompt
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import kotlinx.coroutines.launch

/**
 * Behind the "★ Rate" chip on the widget. Only ever opens a page — the store in the Play build,
 * GitHub in the foss one.
 */
class RateActivity : ComponentActivity() {

    private var choiceMade = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            Telemetry.event(UsageEvent.RATE_CHIP_TAPPED)
            Telemetry.event(UsageEvent.RATE_DIALOG_SHOWN, UsageEvent.PARAM_FROM to UsageEvent.FROM_RATE_CHIP)
        }

        setContent {
            NotesWidgetForMarkdownTheme {
                RateDialog(
                    bodyRes = RatingPrompt.RATE_BODY_RES,
                    rateButtonRes = RatingPrompt.RATE_BUTTON_RES,
                    onChoice = ::onChoice
                )
            }
        }
    }

    private fun onChoice(choice: RateChoice) {
        if (choiceMade) return
        choiceMade = true
        Telemetry.event(
            UsageEvent.RATE_DIALOG_CHOICE,
            UsageEvent.PARAM_CHOICE to choice.eventValue,
            UsageEvent.PARAM_FROM to UsageEvent.FROM_RATE_CHIP
        )
        when (choice) {
            RateChoice.Rate -> {
                RateReminder.markRateTapped(this)
                RatingPrompt.openRatePage(this)
            }
            RateChoice.Idea -> {
                AfterNoteRating.markAnswered(this)
                ProjectLinks.open(this, ProjectLinks.NEW_IDEA_URL)
            }
            RateChoice.Never -> RateReminder.markNeverAgain(this)
            RateChoice.Later -> Unit
        }
        if (!choice.hidesChip) {
            finish()
            return
        }
        // applicationContext: the widget render is async and outlives this activity.
        lifecycleScope.launch {
            NotesWidget.updateAll(applicationContext)
            finish()
        }
    }
}

internal enum class RateChoice(val eventValue: String, val hidesChip: Boolean) {
    Rate(UsageEvent.CHOICE_RATE, hidesChip = true),
    Idea(UsageEvent.CHOICE_IDEA, hidesChip = false),
    Later(UsageEvent.CHOICE_LATER, hidesChip = false),
    Never(UsageEvent.CHOICE_NEVER, hidesChip = true)
}

@Composable
private fun RateDialog(bodyRes: Int, rateButtonRes: Int, onChoice: (RateChoice) -> Unit) {
    Dialog(onDismissRequest = { onChoice(RateChoice.Later) }) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.rate_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { onChoice(RateChoice.Rate) }, modifier = Modifier.fillMaxWidth()) {
                    OneLineLabel(stringResource(rateButtonRes))
                }
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(onClick = { onChoice(RateChoice.Idea) }, modifier = Modifier.fillMaxWidth()) {
                    OneLineLabel(stringResource(R.string.rate_dialog_idea))
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { onChoice(RateChoice.Later) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    OneLineLabel(stringResource(R.string.rate_dialog_later))
                }
                TextButton(
                    onClick = { onChoice(RateChoice.Never) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    OneLineLabel(stringResource(R.string.rate_dialog_never))
                }
            }
        }
    }
}
