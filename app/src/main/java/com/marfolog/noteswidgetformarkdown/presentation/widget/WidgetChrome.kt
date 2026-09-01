package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.domain.model.GitSyncStatus
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardAppearance
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardColor
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity

/**
 * Chrome shared by every widget flavour: the settings/refresh buttons, the bottom action bar
 * with the add buttons, and the git sync chip. Only the list of notes differs between widgets.
 */

// region Widget Action Buttons (Top-Right)

@Composable
internal fun WidgetActionButtons() {
    val setupIntent = Intent(
        androidx.glance.LocalContext.current,
        SetupActivity::class.java
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Box(
        modifier = GlanceModifier.fillMaxSize().padding(4.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(horizontalAlignment = Alignment.End) {
            RoundIconButton(
                icon = R.drawable.ic_refresh,
                contentDescription = "Refresh notes",
                onClickModifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetAction>())
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            RoundIconButton(
                icon = R.drawable.ic_edit,
                contentDescription = "Open widget settings",
                onClickModifier = GlanceModifier.clickable(actionStartActivity(setupIntent))
            )
        }
    }
}

@Composable
internal fun RoundIconButton(
    icon: Int,
    contentDescription: String,
    onClickModifier: GlanceModifier
) {
    Box(
        modifier = GlanceModifier
            .size(40.dp)
            .cornerRadius(20.dp)
            .background(GlanceTheme.colors.widgetBackground)
            .then(onClickModifier),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(20.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
        )
    }
}

// endregion

// region Bottom Bar (actions + sync status)

/** Translucent blue so the bar reads as a layer above the notes, not a solid block. */
internal val BAR_TINT = ColorProvider(Color(0x552196F3))

@Composable
internal fun BottomBar(
    vaultName: String?,
    noteFolderPath: String?,
    gitSyncStatus: GitSyncStatus?
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier
                .cornerRadius(16.dp)
                .background(BAR_TINT)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickActions(vaultName, noteFolderPath)
        }
        Box(modifier = GlanceModifier.defaultWeight()) {}
        // Hidden entirely when the user syncs by other means.
        gitSyncStatus?.let { SyncStatus(it) }
    }
}

@Composable
internal fun SyncStatus(gitSyncStatus: GitSyncStatus) {
    val label = GitSyncLabel.format(gitSyncStatus, System.currentTimeMillis())
    // Explicit traffic-light colors: the theme's primary is not readable as "healthy".
    val color = when (label.severity) {
        GitSyncLabel.Severity.Ok -> ColorProvider(Color(0xFF4CAF50))
        GitSyncLabel.Severity.Stale -> ColorProvider(Color(0xFFFFB300))
        GitSyncLabel.Severity.Problem -> ColorProvider(Color(0xFFE53935))
        GitSyncLabel.Severity.Off -> GlanceTheme.colors.onSurfaceVariant
    }

    // Tapping the chip opens settings, where the full story lives: what is wrong, when it
    // last synced, and the picker for the folder that holds .git.
    val setupIntent = Intent(
        androidx.glance.LocalContext.current,
        SetupActivity::class.java
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Row(
        modifier = GlanceModifier
            .cornerRadius(16.dp)
            .background(BAR_TINT)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clickable(actionStartActivity(setupIntent)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .cornerRadius(4.dp)
                .background(color)
        ) {}
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = label.text,
            style = TextStyle(
                fontSize = 12.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            ),
            maxLines = 1
        )
    }
}

@Composable
internal fun QuickActions(vaultName: String?, noteFolderPath: String?) {
    val filePath = vaultRelativePath(noteFolderPath, "New Note")
    val newNoteUri = buildString {
        append("obsidian://new?")
        if (!vaultName.isNullOrEmpty()) {
            append("vault=")
            append(Uri.encode(vaultName))
            append("&")
        }
        append("file=")
        append(Uri.encode(filePath))
        append("&content=")
    }

    Box(
        modifier = GlanceModifier
            .size(44.dp)
            .cornerRadius(14.dp)
            .background(GlanceTheme.colors.primary)
            .clickable(
                actionStartActivity(
                    Intent(
                        androidx.glance.LocalContext.current,
                        TrampolineActivity::class.java
                    ).putExtra(TrampolineActivity.EXTRA_URI, newNoteUri)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_add),
            contentDescription = "Add note",
            modifier = GlanceModifier.size(24.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
        )
    }
    Spacer(modifier = GlanceModifier.width(8.dp))
    Box(
        modifier = GlanceModifier
            .size(44.dp)
            .cornerRadius(14.dp)
            .background(GlanceTheme.colors.primary)
            .clickable(
                actionStartActivity(
                    Intent(
                        androidx.glance.LocalContext.current,
                        FastNoteActivity::class.java
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_fast_note),
            contentDescription = "Add fast note",
            modifier = GlanceModifier.size(22.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
        )
    }
}

// endregion


/**
 * Content on top, an opaque action bar at the bottom. The bar is a sibling of the content
 * (not an overlay), so the last note is never covered by the buttons.
 */
@Composable
internal fun ConfiguredScaffold(
    vaultName: String?,
    noteFolderPath: String?,
    gitSyncStatus: GitSyncStatus?,
    content: @Composable () -> Unit
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Box(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
            content()
            WidgetActionButtons()
        }
        BottomBar(vaultName, noteFolderPath, gitSyncStatus)
    }
}

/** Obsidian wants the path from the vault root, not a bare file name. */
internal fun vaultRelativePath(noteFolderPath: String?, fileName: String): String =
    if (noteFolderPath.isNullOrEmpty()) fileName else "$noteFolderPath/$fileName"


// region Note card colors (shared by both widget flavours)

@Composable
internal fun NoteCardAppearance.backgroundColor(): ColorProvider {
    return when (color) {
        NoteCardColor.Default -> GlanceTheme.colors.surfaceVariant
        NoteCardColor.Rose -> ColorProvider(Color(0xFFFFDAD6))
        NoteCardColor.Red -> ColorProvider(Color(0xFFFFB4AB))
        NoteCardColor.Amber -> ColorProvider(Color(0xFFFFDEA6))
        NoteCardColor.Orange -> ColorProvider(Color(0xFFFFDCC2))
        NoteCardColor.Mint -> ColorProvider(Color(0xFFBCECCB))
        NoteCardColor.Green -> ColorProvider(Color(0xFFCDEDA3))
        NoteCardColor.Sky -> ColorProvider(Color(0xFFC9E6FF))
        NoteCardColor.Blue -> ColorProvider(Color(0xFFD0E4FF))
        NoteCardColor.Lavender -> ColorProvider(Color(0xFFE7DEFF))
        NoteCardColor.Custom -> ColorProvider(parseCustomColor(customColorHex) ?: Color(0xFFE7E0EC))
    }
}

@Composable
internal fun NoteCardAppearance.contentColor(): ColorProvider {
    return when (color) {
        NoteCardColor.Default -> GlanceTheme.colors.onSurfaceVariant
        NoteCardColor.Rose -> ColorProvider(Color(0xFF410002))
        NoteCardColor.Red -> ColorProvider(Color(0xFF690005))
        NoteCardColor.Amber -> ColorProvider(Color(0xFF2A1800))
        NoteCardColor.Orange -> ColorProvider(Color(0xFF331100))
        NoteCardColor.Mint -> ColorProvider(Color(0xFF00210F))
        NoteCardColor.Green -> ColorProvider(Color(0xFF102000))
        NoteCardColor.Sky -> ColorProvider(Color(0xFF001E30))
        NoteCardColor.Blue -> ColorProvider(Color(0xFF001D36))
        NoteCardColor.Lavender -> ColorProvider(Color(0xFF1D1735))
        NoteCardColor.Custom -> ColorProvider(customContentColor(customColorHex))
    }
}

internal fun parseCustomColor(value: String?): Color? {
    val normalized = value?.trim().orEmpty()
    if (!Regex("^#?[0-9A-Fa-f]{6}$").matches(normalized)) {
        return null
    }
    val hex = normalized.removePrefix("#")
    return Color(("FF$hex").toLong(16))
}

internal fun customContentColor(value: String?): Color {
    val hex = value?.trim()?.removePrefix("#").orEmpty()
    if (!Regex("^[0-9A-Fa-f]{6}$").matches(hex)) {
        return Color(0xFF111111)
    }

    val red = hex.substring(0, 2).toInt(16)
    val green = hex.substring(2, 4).toInt(16)
    val blue = hex.substring(4, 6).toInt(16)
    val luminance = (0.299 * red) + (0.587 * green) + (0.114 * blue)
    return if (luminance > 150) Color(0xFF111111) else Color(0xFFFFFFFF)
}

// endregion
