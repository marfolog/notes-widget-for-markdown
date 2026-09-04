package com.marfolog.noteswidgetformarkdown.presentation.widget

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.glance.appwidget.CircularProgressIndicator
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
import com.marfolog.noteswidgetformarkdown.util.AppLog
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
        AppLog.d("Render", "provideGlance entered at $startedAt")

        // loadWidgetData used to run before provideContent, so the first frame anyone saw was
        // already the finished one — on a slow SAF folder that is a multi-second blank widget
        // with no sign anything is happening, easy to mistake for the refresh being frozen.
        // Composing immediately and loading inside a LaunchedEffect puts a spinner on screen
        // for that window instead. Seeding from WidgetDataCache means that spinner only ever
        // covers the whole widget on the very first render this process has ever done — every
        // refresh after that keeps showing the last known content while it reloads quietly.
        provideContent {
            var data by remember { mutableStateOf(WidgetDataCache.last) }
            var isLoading by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                val fresh = loadWidgetData(context)
                WidgetDataCache.last = fresh
                data = fresh
                isLoading = false
                AppLog.d("Render", "loadWidgetData done in ${System.currentTimeMillis() - startedAt}ms")
            }
            WidgetTheme {
                val current = data
                if (current == null) {
                    LoadingRoot()
                } else {
                    WidgetRoot(
                        current.state,
                        current.vaultName,
                        current.noteFolderPath,
                        current.cardSettings,
                        current.gitSyncStatus,
                        isLoading
                    )
                }
            }
        }
        AppLog.d("Render", "provideGlance rendered in ${System.currentTimeMillis() - startedAt}ms")
    }

    companion object {
        private const val TAG = "NotesWidget"

        suspend fun updateAll(context: Context) {
            val startedAt = System.currentTimeMillis()
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(NotesWidget::class.java)
            ids.forEach { id ->
                // See forceFreshGlanceSession: makes update() start a new session instead of
                // trusting one that may be wedged. A tried-and-failed 600ms retry used to be
                // here instead — logged proof it did not help is in issue #4.
                forceFreshGlanceSession(context, manager, id)
                NotesWidget().update(context, id)
            }
            // There used to be a second refresh here too: a plain APPWIDGET_UPDATE broadcast,
            // sent as a fallback for when update() alone could not be trusted to land. Now that
            // forceFreshGlanceSession makes update() itself reliable, that fallback only added a
            // redundant, visibly delayed second recompose — the loading spinner flashing across
            // the whole widget a moment after it had already shown the right data.
            // Both flavours show the same folder, so they always refresh together.
            NotesListWidget.updateAll(context)
            AppLog.d("Render", "updateAll requested for ${ids.size} widgets in ${System.currentTimeMillis() - startedAt}ms")
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
    gitSyncStatus: GitSyncStatus?,
    isLoading: Boolean = false
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
            is WidgetState.Success -> ConfiguredContent(state.notes, vaultName, noteFolderPath, cardSettings, gitSyncStatus, isLoading)
            is WidgetState.Empty -> ConfiguredContentEmpty(vaultName, noteFolderPath, gitSyncStatus, isLoading)
            is WidgetState.Error -> ConfiguredContentError(state.message, vaultName, noteFolderPath, gitSyncStatus, isLoading)
        }
    }
}

// endregion

// region Loading State

/**
 * First frame of every provideGlance call, shown for as long as [loadWidgetData] takes (SAF
 * folder reads have been over a second on device). Without this the widget was blank — same
 * area, same background — for that whole stretch, which reads as frozen rather than working.
 */
@Composable
private fun LoadingRoot() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = GlanceTheme.colors.primary)
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
    gitSyncStatus: GitSyncStatus?,
    isLoading: Boolean = false
) {
    ConfiguredScaffold(vaultName, noteFolderPath, gitSyncStatus, isLoading) {
        NotesList(notes, vaultName, noteFolderPath, cardSettings)
    }
}

@Composable
private fun ConfiguredContentEmpty(
    vaultName: String?,
    noteFolderPath: String?,
    gitSyncStatus: GitSyncStatus?,
    isLoading: Boolean = false
) {
    ConfiguredScaffold(vaultName, noteFolderPath, gitSyncStatus, isLoading) {
        EmptyContent()
    }
}

@Composable
private fun ConfiguredContentError(
    message: String,
    vaultName: String?,
    noteFolderPath: String?,
    gitSyncStatus: GitSyncStatus?,
    isLoading: Boolean = false
) {
    ConfiguredScaffold(vaultName, noteFolderPath, gitSyncStatus, isLoading) {
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
    val openIntent = noteOpenIntent(LocalContext.current, note, vaultName, noteFolderPath)

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(appearance.size.heightDp.dp)
            .cornerRadius(16.dp)
            .background(appearance.backgroundColor())
            .clickable(actionStartActivity(openIntent))
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
