package com.marfolog.noteswidgetformarkdown.presentation.onboarding

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marfolog.noteswidgetformarkdown.R

/** Buttons or just notes; [choice] is null until the user picks, and Continue waits for that. */
@Composable
internal fun ColumnScope.ButtonsStep(
    choice: Boolean?,
    onChoose: (Boolean) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    StepLayout(
        titleRes = R.string.onboarding_buttons_title,
        bodyRes = R.string.onboarding_buttons_body,
        content = {
            ButtonsChoiceCards(
                choice = choice,
                onChoose = onChoose,
                showTitleRes = R.string.onboarding_buttons_show,
                hideTitleRes = R.string.onboarding_buttons_hide
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.onboarding_buttons_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        buttons = {
            Button(onClick = onContinue, enabled = choice != null, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_continue))
            }
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                Text(stringResource(R.string.onboarding_back))
            }
        }
    )
}
