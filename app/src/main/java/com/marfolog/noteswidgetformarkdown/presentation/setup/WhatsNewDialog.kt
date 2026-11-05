package com.marfolog.noteswidgetformarkdown.presentation.setup

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.marfolog.noteswidgetformarkdown.BuildConfig
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.presentation.onboarding.ButtonsChoiceCards
import com.marfolog.noteswidgetformarkdown.presentation.widget.AddButtons
import com.marfolog.noteswidgetformarkdown.presentation.widget.NotesWidget
import com.marfolog.noteswidgetformarkdown.util.RateReminder
import com.marfolog.noteswidgetformarkdown.util.RatingPrompt
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import com.marfolog.noteswidgetformarkdown.util.WhatsNew
import com.marfolog.noteswidgetformarkdown.util.AppClock
import com.marfolog.noteswidgetformarkdown.util.WhatsNewIcon
import com.marfolog.noteswidgetformarkdown.util.WhatsNewItem
import com.marfolog.noteswidgetformarkdown.util.WhatsNewPage
import com.marfolog.noteswidgetformarkdown.util.WhatsNewReminder
import kotlinx.coroutines.launch

/**
 * The "What's new" dialog: the page [WhatsNew.firstPage] says, then the news. The page is saved with
 * the activity, so a rotation or a killed process comes back to the page it left. The screen behind
 * it is told of a choice about the buttons, so its switch does not go on showing the old one.
 */
@Composable
internal fun WhatsNewDialogHost(onButtonsChosen: (shown: Boolean) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val release = WhatsNew.release(BuildConfig.VERSION_NAME)
    var page by rememberSaveable { mutableStateOf(WhatsNew.firstPage(BuildConfig.VERSION_NAME, AddButtons.choice(context))) }
    var reportedPage by rememberSaveable { mutableStateOf<WhatsNewPage?>(null) }

    LaunchedEffect(page) {
        if (reportedPage == page) return@LaunchedEffect
        reportedPage = page
        Telemetry.event(
            UsageEvent.WHATS_NEW_SHOWN,
            UsageEvent.PARAM_VERSION to release,
            UsageEvent.PARAM_STEP to page.eventValue
        )
    }

    fun chooseButtons(shown: Boolean) {
        AddButtons.setShown(context, shown)
        Telemetry.event(UsageEvent.SETTING_CHANGED, *UsageEvent.addButtonsChanged(shown, UsageEvent.FROM_WHATS_NEW))
        // Seen only now: leaving the question open must bring the dialog back, not lose it.
        WhatsNewReminder.markShown(context, AppClock.now())
        // The widget changes with the choice, and the rate chip keeps its distance from this dialog.
        scope.launch { NotesWidget.updateAll(context) }
        onButtonsChosen(shown)
        page = WhatsNewPage.News
    }

    fun close(choice: String) {
        Telemetry.event(
            UsageEvent.WHATS_NEW_CHOICE,
            UsageEvent.PARAM_CHOICE to choice,
            UsageEvent.PARAM_VERSION to release
        )
        onClose()
    }

    when (page) {
        WhatsNewPage.ButtonsChoice -> WhatsNewButtonsDialog(release = release, onChoose = ::chooseButtons)
        WhatsNewPage.News -> WhatsNewDialog(
            release = release,
            items = WhatsNew.itemsFor(release),
            rateButtonRes = RatingPrompt.RATE_BUTTON_RES,
            footerRes = RatingPrompt.WHATS_NEW_FOOTER_RES,
            onRate = {
                RateReminder.markRateTapped(context)
                RatingPrompt.openRatePage(context)
                close(UsageEvent.CHOICE_RATE)
            },
            onLater = { close(UsageEvent.CHOICE_LATER) },
            onDismiss = { close(UsageEvent.CHOICE_DISMISSED) }
        )
    }
}

/** A question, not a notice: nothing closes it but an answer, and neither answer is preselected. */
@Composable
private fun WhatsNewButtonsDialog(release: String, onChoose: (shown: Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.whats_new_title, release)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.whats_new_buttons_intro), style = MaterialTheme.typography.bodyMedium)
                ButtonsChoiceCards(
                    choice = null,
                    onChoose = onChoose,
                    showTitleRes = R.string.whats_new_buttons_keep,
                    hideTitleRes = R.string.whats_new_buttons_hide,
                    compact = true
                )
                Text(
                    text = stringResource(R.string.onboarding_buttons_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun WhatsNewDialog(
    release: String,
    items: List<WhatsNewItem>,
    @StringRes rateButtonRes: Int,
    @StringRes footerRes: Int,
    onRate: () -> Unit,
    onLater: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.whats_new_title, release)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.forEach { item -> WhatsNewRow(item) }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(footerRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onRate) { Text(stringResource(rateButtonRes)) }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text(stringResource(R.string.whats_new_later)) }
        }
    )
}

@Composable
private fun WhatsNewRow(item: WhatsNewItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = item.icon.vector(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(stringResource(item.textRes), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun WhatsNewIcon.vector(): ImageVector = when (this) {
    WhatsNewIcon.Palette -> Icons.Outlined.Palette
    WhatsNewIcon.Checklist -> Icons.Outlined.Checklist
    WhatsNewIcon.Chat -> Icons.Outlined.ChatBubbleOutline
    WhatsNewIcon.Translate -> Icons.Outlined.Translate
    WhatsNewIcon.Folder -> Icons.Outlined.FolderOpen
    WhatsNewIcon.Sync -> Icons.Outlined.Sync
}
