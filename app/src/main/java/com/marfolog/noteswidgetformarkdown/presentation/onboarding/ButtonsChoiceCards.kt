package com.marfolog.noteswidgetformarkdown.presentation.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marfolog.noteswidgetformarkdown.R

/**
 * The two answers to "buttons on the widget or not", each with a picture of what it leaves. Shared by
 * the first-launch guide and the "What's new" dialog, which word the first answer differently and,
 * being narrower, ask for the [compact] cards: [choice] is null until the user picks, and neither
 * card is preselected.
 */
@Composable
internal fun ButtonsChoiceCards(
    choice: Boolean?,
    onChoose: (shown: Boolean) -> Unit,
    @StringRes showTitleRes: Int,
    @StringRes hideTitleRes: Int,
    compact: Boolean = false
) {
    val previewWidth = if (compact) 84.dp else 112.dp
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceCard(
            selected = choice == true,
            titleRes = showTitleRes,
            descRes = R.string.onboarding_buttons_show_desc,
            onSelect = { onChoose(true) },
            compact = compact,
            preview = { MiniWidgetBottom(showButtons = true, width = previewWidth) }
        )
        ChoiceCard(
            selected = choice == false,
            titleRes = hideTitleRes,
            descRes = R.string.onboarding_buttons_hide_desc,
            onSelect = { onChoose(false) },
            compact = compact,
            preview = { MiniWidgetBottom(showButtons = false, width = previewWidth) }
        )
    }
}
