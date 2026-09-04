package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Intent
import android.net.Uri
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
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
import com.marfolog.noteswidgetformarkdown.ui.theme.NoteCardPalette

/**
 * Chrome shared by every widget flavour: the settings/refresh buttons, the bottom action bar
 * with the add buttons, and the git sync chip. Only the list of notes differs between widgets.
 */

// region Widget Action Buttons (Top-Right)

@Composable
internal fun WidgetActionButtons(isLoading: Boolean = false) {
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
            if (isLoading) {
                // Same slot the refresh button sits in, so a refresh in flight reads as "in
                // progress" without the rest of the widget going blank to show it.
                Box(
                    modifier = GlanceModifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = GlanceModifier.size(20.dp),
                        color = GlanceTheme.colors.onSurface
                    )
                }
            } else {
                RoundIconButton(
                    icon = R.drawable.ic_refresh,
                    contentDescription = LocalContext.current.getString(R.string.action_refresh),
                    onClickModifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetAction>())
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            RoundIconButton(
                icon = R.drawable.ic_edit,
                contentDescription = LocalContext.current.getString(R.string.action_open_settings),
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
    val context = LocalContext.current
    val staleHours = context
        .getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
        .getInt(SetupActivity.KEY_STALE_HOURS, GitSyncLabel.DEFAULT_STALE_HOURS)
    val label = GitSyncLabel.format(gitSyncStatus, System.currentTimeMillis(), staleHours)
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
            text = label.arg
                ?.let { context.getString(label.textRes, it) }
                ?: context.getString(label.textRes),
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
    Box(
        modifier = GlanceModifier
            .size(44.dp)
            .cornerRadius(14.dp)
            .background(GlanceTheme.colors.primary)
            .clickable(
                actionStartActivity(
                    Intent(androidx.glance.LocalContext.current, AddNoteActivity::class.java)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_add),
            contentDescription = LocalContext.current.getString(R.string.action_add_note),
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
            contentDescription = LocalContext.current.getString(R.string.action_add_fast_note),
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
    isLoading: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Box(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
            content()
            WidgetActionButtons(isLoading)
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
    val custom = if (color == NoteCardColor.Custom) {
        NoteCardPalette.parseCustom(customColorHex) ?: Color(0xFFE7E0EC)
    } else {
        null
    }
    val fixed = custom ?: NoteCardPalette.background(color)
    return fixed?.let { ColorProvider(it) } ?: GlanceTheme.colors.surfaceVariant
}

@Composable
internal fun NoteCardAppearance.contentColor(): ColorProvider {
    val custom = if (color == NoteCardColor.Custom) {
        NoteCardPalette.contentForCustom(customColorHex)
    } else {
        null
    }
    val fixed = custom ?: NoteCardPalette.content(color)
    return fixed?.let { ColorProvider(it) } ?: GlanceTheme.colors.onSurfaceVariant
}

// endregion
