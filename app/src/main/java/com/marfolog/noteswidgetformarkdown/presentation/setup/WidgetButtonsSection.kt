package com.marfolog.noteswidgetformarkdown.presentation.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.marfolog.noteswidgetformarkdown.R

/** The add and fast note buttons in the widget's bottom bar, on or off. The guide and the "What's new" dialog ask the same question. */
@Composable
internal fun WidgetButtonsSection(shown: Boolean, onChanged: (Boolean) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.widget_buttons_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = shown, role = Role.Switch, onValueChange = onChanged),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.add_buttons_setting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = shown, onCheckedChange = null)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.add_buttons_setting_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
