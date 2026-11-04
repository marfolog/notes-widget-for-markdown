package com.marfolog.noteswidgetformarkdown.presentation.consent

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.util.ReportingConsent
import com.marfolog.noteswidgetformarkdown.util.TelemetrySwitch

/**
 * The reporting question as a dialog, for the places that ask it in passing: settings and the
 * moment after a saved note. Dismissing it counts as no.
 */
@Composable
internal fun ReportingConsentDialog(onAnswered: (agreed: Boolean) -> Unit) {
    val context = LocalContext.current
    fun answer(agreed: Boolean) {
        ReportingConsent.answer(TelemetrySwitch(context), agreed)
        AppLog.i("Consent", "Reporting switched ${if (agreed) "on" else "off"} by the dialog")
        onAnswered(agreed)
    }
    AlertDialog(
        onDismissRequest = { answer(false) },
        title = { Text(stringResource(R.string.consent_prompt_title)) },
        text = { Text(stringResource(R.string.consent_prompt_body)) },
        confirmButton = {
            TextButton(onClick = { answer(true) }) { Text(stringResource(R.string.consent_prompt_yes)) }
        },
        dismissButton = {
            TextButton(onClick = { answer(false) }) { Text(stringResource(R.string.consent_prompt_no)) }
        }
    )
}
