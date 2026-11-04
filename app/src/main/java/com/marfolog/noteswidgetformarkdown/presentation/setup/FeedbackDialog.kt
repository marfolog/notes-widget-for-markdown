package com.marfolog.noteswidgetformarkdown.presentation.setup

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.util.FeedbackForm
import com.marfolog.noteswidgetformarkdown.util.FeedbackSendResult
import com.marfolog.noteswidgetformarkdown.util.FeedbackSender
import com.marfolog.noteswidgetformarkdown.util.FeedbackThumbs
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.UsageEvent

private val FeedbackFormSaver = listSaver<FeedbackForm, Any?>(
    save = { listOf(it.thumbs?.value, it.text) },
    restore = { saved ->
        FeedbackForm(
            thumbs = FeedbackThumbs.from(saved[0] as String?),
            text = saved[1] as String
        )
    }
)

/** Holds the form across rotation and carries out the three ways out of the dialog. */
@Composable
internal fun FeedbackDialogHost(onClose: () -> Unit) {
    val context = LocalContext.current
    val sender: FeedbackSendViewModel = viewModel()
    var form by rememberSaveable(stateSaver = FeedbackFormSaver) { mutableStateOf(FeedbackForm()) }
    var errorRes by rememberSaveable { mutableStateOf<Int?>(null) }

    fun close(choice: String) {
        Telemetry.event(UsageEvent.FEEDBACK_DIALOG_CHOICE, UsageEvent.PARAM_CHOICE to choice)
        onClose()
    }

    LaunchedEffect(sender.outcome) {
        val outcome = sender.outcome ?: return@LaunchedEffect
        sender.outcomeHandled()
        when (outcome.result) {
            FeedbackSendResult.Sent -> {
                UsageEvent.feedbackSent(outcome.form).forEach { (name, param) -> Telemetry.event(name, param) }
                Toast.makeText(context, R.string.feedback_thanks, Toast.LENGTH_SHORT).show()
                onClose()
            }
            FeedbackSendResult.Failed -> errorRes = FeedbackSender.FAILED_RES
            FeedbackSendResult.TooSoon -> errorRes = R.string.feedback_too_soon
            FeedbackSendResult.Pending -> errorRes = R.string.feedback_pending
        }
    }

    FeedbackDialog(
        form = form,
        onFormChanged = {
            form = it
            errorRes = null
        },
        sending = sender.sending,
        errorRes = errorRes,
        footerRes = FeedbackSender.FOOTER_RES,
        onSend = {
            errorRes = null
            sender.send(form)
        },
        onLater = { close(UsageEvent.CHOICE_LATER) },
        onNever = { close(UsageEvent.CHOICE_NEVER) }
    )
}

@Composable
private fun FeedbackDialog(
    form: FeedbackForm,
    onFormChanged: (FeedbackForm) -> Unit,
    sending: Boolean,
    @StringRes errorRes: Int?,
    @StringRes footerRes: Int,
    onSend: () -> Unit,
    onLater: () -> Unit,
    onNever: () -> Unit
) {
    AlertDialog(
        // Only back closes it: a stray tap beside the dialog would throw away what was typed.
        onDismissRequest = { if (!sending) onLater() },
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.feedback_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.feedback_body))
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.feedback_how), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThumbChip(FeedbackThumbs.Up, "👍", R.string.feedback_thumbs_up, form, onFormChanged)
                    ThumbChip(FeedbackThumbs.Down, "👎", R.string.feedback_thumbs_down, form, onFormChanged)
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = form.text,
                    onValueChange = { onFormChanged(form.withText(it)) },
                    label = { Text(stringResource(R.string.feedback_text)) },
                    supportingText = {
                        Text(
                            stringResource(R.string.feedback_text_counter, form.text.length, FeedbackForm.MAX_TEXT_LENGTH)
                        )
                    },
                    minLines = 2,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorRes != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(errorRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(footerRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onSend, enabled = form.canSend && !sending) {
                Text(stringResource(R.string.feedback_send))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onNever, enabled = !sending) { Text(stringResource(R.string.feedback_never)) }
                TextButton(onClick = onLater, enabled = !sending) { Text(stringResource(R.string.feedback_later)) }
            }
        }
    )
}

@Composable
private fun ThumbChip(
    thumbs: FeedbackThumbs,
    emoji: String,
    @StringRes descriptionRes: Int,
    form: FeedbackForm,
    onFormChanged: (FeedbackForm) -> Unit
) {
    val description = stringResource(descriptionRes)
    FilterChip(
        selected = form.thumbs == thumbs,
        onClick = { onFormChanged(form.withThumbs(thumbs)) },
        label = { Text(emoji, style = MaterialTheme.typography.titleMedium) },
        modifier = Modifier.semantics { contentDescription = description }
    )
}
