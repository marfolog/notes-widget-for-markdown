package com.marfolog.noteswidgetformarkdown.presentation.onboarding

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.presentation.widget.OneLineLabel

/**
 * The reporting question, in the words of the dialog it replaces. Each answer is a single tap that
 * also moves on, and neither is preselected or ticked: a consent that is filled in beforehand is not
 * valid in the EU.
 */
@Composable
internal fun ColumnScope.ConsentStep(
    onAnswer: (agreed: Boolean) -> Unit,
    onBack: () -> Unit
) {
    StepLayout(
        titleRes = R.string.consent_prompt_title,
        bodyRes = R.string.consent_prompt_body,
        content = {},
        buttons = {
            Button(onClick = { onAnswer(true) }, modifier = Modifier.fillMaxWidth()) {
                OneLineLabel(stringResource(R.string.onboarding_consent_share))
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = { onAnswer(false) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                OneLineLabel(stringResource(R.string.onboarding_consent_no))
            }
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                Text(stringResource(R.string.onboarding_back))
            }
        }
    )
}
