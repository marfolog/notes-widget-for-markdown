package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.data.preferences.NoteCardSettingsStore
import com.marfolog.noteswidgetformarkdown.data.repository.GitSyncStatusReader
import com.marfolog.noteswidgetformarkdown.data.repository.ObsidianVaultLocator
import com.marfolog.noteswidgetformarkdown.domain.model.GitSyncStatus
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardAppearance
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardColor
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.usecase.GetNotesUseCase
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.ui.theme.WidgetTheme
import kotlinx.coroutines.flow.firstOrNull
import org.koin.java.KoinJavaComponent.get

class NotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val startedAt = System.currentTimeMillis()
        val data = loadWidgetData(context)

        provideContent {
            WidgetTheme {
                WidgetRoot(data.state, data.vaultName, data.noteFolderPath, data.cardSettings, data.gitSyncStatus)
            }
        }
        Log.d(TAG, "Rendered widget in ${System.currentTimeMillis() - startedAt}ms")
    }

    companion object {
        private const val TAG = "NotesWidget"

        suspend fun updateAll(context: Context) {
            val startedAt = System.currentTimeMillis()
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(NotesWidget::class.java)
            ids.forEach { id ->
                NotesWidget().update(context, id)
            }
            notifyReceiver(context)
            // Both flavours show the same folder, so they always refresh together.
            NotesListWidget.updateAll(context)
            Log.d(TAG, "Requested update for ${ids.size} widgets in ${System.currentTimeMillis() - startedAt}ms")
        }

        /**
         * Glance drives updates through a WorkManager session that can get stuck — the update is
         * accepted but nothing recomposes until the process restarts. A plain APPWIDGET_UPDATE
         * broadcast goes through the framework instead, so at least one of the two lands.
         */
        private fun notifyReceiver(context: Context) {
            runCatching {
                val widgetManager = AppWidgetManager.getInstance(context) ?: return
                val component = ComponentName(context, NotesWidgetReceiver::class.java)
                val widgetIds = widgetManager.getAppWidgetIds(component)
                if (widgetIds.isEmpty()) {
                    return
                }
                context.sendBroadcast(
                    Intent(context, NotesWidgetReceiver::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                    }
                )
            }.onFailure { Log.w(TAG, "Fallback widget broadcast failed", it) }
        }
    }
}

// region Root

@Composable
private fun WidgetRoot(
    state: WidgetState,
    vaultName: String?,
    noteFolderPath: String?,
    cardSettings: Map<String, NoteCardAppearance>,
    gitSyncStatus: GitSyncStatus?
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(16.dp)
    ) {
        when (state) {
            is WidgetState.Uninitialized -> UninitializedContent()
            is WidgetState.PermissionLost -> PermissionLostContent()
            is WidgetState.Success -> ConfiguredContent(state.notes, vaultName, noteFolderPath, cardSettings, gitSyncStatus)
            is WidgetState.Empty -> ConfiguredContentEmpty(vaultName, noteFolderPath, gitSyncStatus)
            is WidgetState.Error -> ConfiguredContentError(state.message, vaultName, noteFolderPath, gitSyncStatus)
        }
    }
}

// endregion

// region Uninitialized State

@Composable
private fun UninitializedContent() {
    val setupIntent = Intent(
        androidx.glance.LocalContext.current,
        SetupActivity::class.java
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.surfaceVariant)
            .clickable(actionStartActivity(setupIntent))
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_setup),
            contentDescription = LocalContext.current.getString(R.string.action_setup),
            modifier = GlanceModifier.size(48.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
        )
        Spacer(modifier = GlanceModifier.height(16.dp))
        Text(
            text = LocalContext.current.getString(R.string.widget_setup_required),
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = LocalContext.current.getString(R.string.widget_setup_hint),
            style = TextStyle(
                fontSize = 14.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
    }
}

// endregion

// region Permission Lost State

@Composable
private fun PermissionLostContent() {
    val setupIntent = Intent(
        androidx.glance.LocalContext.current,
        SetupActivity::class.java
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.surfaceVariant)
            .clickable(actionStartActivity(setupIntent))
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_error),
            contentDescription = LocalContext.current.getString(R.string.action_error),
            modifier = GlanceModifier.size(48.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.error)
        )
        Spacer(modifier = GlanceModifier.height(16.dp))
        Text(
            text = LocalContext.current.getString(R.string.widget_permission_lost),
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = GlanceTheme.colors.error
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = LocalContext.current.getString(R.string.widget_permission_hint),
            style = TextStyle(
                fontSize = 14.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
    }
}

// endregion

// region Configured States (Content + Refresh + FAB layers)

@Composable
private fun ConfiguredContent(
    notes: List<NoteSummary>,
    vaultName: String?,
    noteFolderPath: String?,
    cardSettings: Map<String, NoteCardAppearance>,
    gitSyncStatus: GitSyncStatus?
) {
    ConfiguredScaffold(vaultName, noteFolderPath, gitSyncStatus) {
        NotesList(notes, vaultName, noteFolderPath, cardSettings)
    }
}

@Composable
private fun ConfiguredContentEmpty(
    vaultName: String?,
    noteFolderPath: String?,
    gitSyncStatus: GitSyncStatus?
) {
    ConfiguredScaffold(vaultName, noteFolderPath, gitSyncStatus) {
        EmptyContent()
    }
}

@Composable
private fun ConfiguredContentError(
    message: String,
    vaultName: String?,
    noteFolderPath: String?,
    gitSyncStatus: GitSyncStatus?
) {
    ConfiguredScaffold(vaultName, noteFolderPath, gitSyncStatus) {
        ErrorContent(message)
    }
}

// endregion

// region Notes List

@Composable
private fun NotesList(
    notes: List<NoteSummary>,
    vaultName: String?,
    noteFolderPath: String?,
    cardSettings: Map<String, NoteCardAppearance>
) {
    LazyColumn(
        modifier = GlanceModifier
            .fillMaxSize()
    ) {
        items(notes, itemId = { note ->
            val appearance = cardSettings[note.fileUri] ?: NoteCardAppearance()
            "${note.id}:${appearance.size.storageValue}:${appearance.color.storageValue}:${appearance.textSize.storageValue}:${appearance.customColorHex.orEmpty()}"
                .hashCode()
                .toLong()
        }) { note ->
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                NoteCard(
                    note,
                    vaultName,
                    noteFolderPath,
                    cardSettings[note.fileUri] ?: NoteCardAppearance()
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteSummary,
    vaultName: String?,
    noteFolderPath: String?,
    appearance: NoteCardAppearance
) {
    val obsidianUri = buildString {
        append("obsidian://open?")
        if (!vaultName.isNullOrEmpty()) {
            append("vault=${Uri.encode(vaultName)}&")
        }
        append("file=${Uri.encode(vaultRelativePath(noteFolderPath, note.fileName))}")
    }
    val obsidianIntent = Intent(Intent.ACTION_VIEW, Uri.parse(obsidianUri))

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(appearance.size.heightDp.dp)
            .cornerRadius(16.dp)
            .background(appearance.backgroundColor())
            .clickable(actionStartActivity(obsidianIntent))
            .padding(12.dp)
    ) {
        Text(
            text = note.title,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = appearance.textSize.titleSp.sp,
                color = appearance.contentColor()
            ),
            maxLines = 1
        )
        Text(
            text = note.preview,
            style = TextStyle(
                fontSize = appearance.textSize.previewSp.sp,
                color = appearance.contentColor()
            ),
            maxLines = appearance.size.previewMaxLines,
            modifier = GlanceModifier.padding(top = 4.dp)
        )
    }
}

// endregion

// region Empty & Error Content

@Composable
private fun EmptyContent(modifier: GlanceModifier = GlanceModifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = LocalContext.current.getString(R.string.widget_no_notes),
            style = TextStyle(
                fontSize = 14.sp,
                color = GlanceTheme.colors.onSurface
            )
        )
    }
}

@Composable
private fun ErrorContent(message: String, modifier: GlanceModifier = GlanceModifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                provider = ImageProvider(R.drawable.ic_error),
                contentDescription = LocalContext.current.getString(R.string.action_error),
                modifier = GlanceModifier.size(32.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.error)
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = message,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.error
                )
            )
        }
    }
}

// endregion
