package com.marfolog.noteswidgetformarkdown.presentation.onboarding

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marfolog.noteswidgetformarkdown.R

@Composable
internal fun ColumnScope.WelcomeStep(onGetStarted: () -> Unit) {
    val previewDescription = stringResource(R.string.onboarding_welcome_preview)
    StepLayout(
        titleRes = R.string.onboarding_welcome_title,
        bodyRes = R.string.onboarding_welcome_body,
        content = {
            MiniWidget(
                showButtons = true,
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .align(Alignment.CenterHorizontally)
                    .semantics { contentDescription = previewDescription }
            )
        },
        buttons = {
            Button(onClick = onGetStarted, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_get_started))
            }
        }
    )
}
