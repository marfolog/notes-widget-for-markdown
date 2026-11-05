package com.marfolog.noteswidgetformarkdown.presentation.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.util.UsageEvent

/** Where the banner is in its one visit: asking, or thanking once the answer was yes. */
internal enum class InterestBannerState {
    Hidden, Ask, Thanks;

    /**
     * What closing the banner counts as: a dismissal while it still asks, nothing once the question
     * was answered, so one person is never counted as "yes" and "dismissed" both.
     */
    fun dismissAnswer(): String? = if (this == Ask) UsageEvent.ANSWER_DISMISSED else null
}

/**
 * The question at the top of settings whether separate widgets for separate folders would be used.
 * It promises nothing, so the thanks after "yes" says only that the answer was noted.
 */
@Composable
internal fun FeatureInterestBanner(
    state: InterestBannerState,
    onYes: () -> Unit,
    onNo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state == InterestBannerState.Hidden) return
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Box {
            Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(
                            if (state == InterestBannerState.Thanks) R.string.feature_interest_thanks
                            else R.string.feature_interest_title
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        // Room for the close button in the corner.
                        modifier = Modifier.weight(1f).padding(end = 32.dp)
                    )
                }
                if (state == InterestBannerState.Ask) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.feature_interest_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = onYes) { Text(stringResource(R.string.feature_interest_yes), maxLines = 1) }
                        TextButton(onClick = onNo) { Text(stringResource(R.string.feature_interest_no), maxLines = 1) }
                    }
                    Text(
                        text = stringResource(R.string.feature_interest_anonymous),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.feature_interest_close),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
