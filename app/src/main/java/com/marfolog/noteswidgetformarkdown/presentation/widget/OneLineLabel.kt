package com.marfolog.noteswidgetformarkdown.presentation.widget

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private val SMALLEST = 9.sp
private const val SHRINK_STEP = 0.92f

/**
 * A button label that never wraps: when it does not fit the width, in a long language or at a large
 * font size, it gets a little smaller until it does, rather than breaking onto a second line.
 */
@Composable
internal fun OneLineLabel(text: String, style: TextStyle = MaterialTheme.typography.labelLarge) {
    var fontSize: TextUnit by remember(text, style) { mutableStateOf(style.fontSize) }
    Text(
        text = text,
        style = style,
        fontSize = fontSize,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { layout ->
            if (layout.hasVisualOverflow && fontSize > SMALLEST) fontSize *= SHRINK_STEP
        }
    )
}
