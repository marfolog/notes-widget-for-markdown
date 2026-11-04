package com.marfolog.noteswidgetformarkdown.presentation.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardColor
import com.marfolog.noteswidgetformarkdown.ui.theme.NoteCardPalette

private val CARD_COLORS = listOf(NoteCardColor.Amber, NoteCardColor.Mint, NoteCardColor.Sky)
private val SYNC_DOT = Color(0xFF4CAF50)

/**
 * The widget drawn in the guide's own colours: three note cards and, when [showButtons] is on, the
 * bar with the two add buttons. Drawn rather than shipped as an image, so it follows light and dark
 * and has no edge of its own to show against either.
 */
@Composable
internal fun MiniWidget(showButtons: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CARD_COLORS.forEach { MiniCard(it, height = 56.dp, bodyLines = 2) }
            if (showButtons) MiniBar(buttonSize = 40.dp, withChip = true)
        }
    }
}

/** Just the lower part of the widget, small enough to sit beside a label. */
@Composable
internal fun MiniWidgetBottom(showButtons: Boolean, modifier: Modifier = Modifier, width: Dp = 112.dp) {
    Surface(
        modifier = modifier.width(width),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CARD_COLORS.take(if (showButtons) 2 else 3).forEach { MiniCard(it, height = 26.dp, bodyLines = 0) }
            if (showButtons) MiniBar(buttonSize = 28.dp, withChip = false)
        }
    }
}

@Composable
private fun MiniCard(color: NoteCardColor, height: Dp, bodyLines: Int) {
    val ink = NoteCardPalette.content(color) ?: MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(NoteCardPalette.background(color) ?: MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = if (bodyLines == 0) 9.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        MiniLine(ink.copy(alpha = 0.85f), Modifier.fillMaxWidth(0.4f).height(8.dp))
        repeat(bodyLines) {
            MiniLine(ink.copy(alpha = 0.4f), Modifier.fillMaxWidth(if (it % 2 == 0) 0.85f else 0.6f).height(6.dp))
        }
    }
}

@Composable
private fun MiniLine(color: Color, modifier: Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(3.dp)).background(color))
}

@Composable
private fun MiniBar(buttonSize: Dp, withChip: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            MiniButton(R.drawable.ic_add, buttonSize)
            MiniButton(R.drawable.ic_fast_note, buttonSize)
        }
        if (withChip) {
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SYNC_DOT))
                Spacer(modifier = Modifier.width(6.dp))
                MiniLine(
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                    Modifier.width(36.dp).height(6.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniButton(icon: Int, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(size / 2)
        )
    }
}
