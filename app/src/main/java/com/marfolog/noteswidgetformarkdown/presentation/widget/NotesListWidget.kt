package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardAppearance
import com.marfolog.noteswidgetformarkdown.ui.theme.WidgetTheme
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary

/**
 * The compact flavour: one note name per line, in the same colour the card widget uses.
 * Same data, actions and git chip as the card widget — only denser, so more notes fit.
 */
class NotesListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val startedAt = System.currentTimeMillis()
        val data = loadWidgetData(context)

        provideContent {
            WidgetTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(12.dp)
                ) {
                    ConfiguredScaffold(data.vaultName, data.noteFolderPath, data.gitSyncStatus) {
                        when (val state = data.state) {
                            is WidgetState.Success -> NoteNameList(
                                notes = state.notes,
                                vaultName = data.vaultName,
                                noteFolderPath = data.noteFolderPath,
                                cardSettings = data.cardSettings
                            )
                            else -> StatusMessage(state)
                        }
                    }
                }
            }
        }
        Log.d(TAG, "Rendered list widget in ${System.currentTimeMillis() - startedAt}ms")
    }

    companion object {
        private const val TAG = "NotesListWidget"

        suspend fun updateAll(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(NotesListWidget::class.java).forEach { id ->
                NotesListWidget().update(context, id)
            }
        }
    }
}

@Composable
private fun NoteNameList(
    notes: List<NoteSummary>,
    vaultName: String?,
    noteFolderPath: String?,
    cardSettings: Map<String, NoteCardAppearance>
) {
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        items(notes, itemId = { note ->
            val appearance = cardSettings[note.fileUri] ?: NoteCardAppearance()
            "${note.id}:${appearance.color.storageValue}:${appearance.customColorHex.orEmpty()}"
                .hashCode()
                .toLong()
        }) { note ->
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                NoteNameRow(
                    note = note,
                    vaultName = vaultName,
                    noteFolderPath = noteFolderPath,
                    appearance = cardSettings[note.fileUri] ?: NoteCardAppearance()
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun NoteNameRow(
    note: NoteSummary,
    vaultName: String?,
    noteFolderPath: String?,
    appearance: NoteCardAppearance
) {
    val openIntent = noteOpenIntent(LocalContext.current, note, vaultName, noteFolderPath)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(12.dp)
            .background(appearance.backgroundColor())
            .clickable(actionStartActivity(openIntent))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = note.title,
            style = TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = appearance.contentColor()
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
    }
}

@Composable
private fun StatusMessage(state: WidgetState) {
    val message = when (state) {
        is WidgetState.Uninitialized -> LocalContext.current.getString(R.string.widget_pick_folder_hint)
        is WidgetState.PermissionLost -> LocalContext.current.getString(R.string.widget_permission_lost_short)
        is WidgetState.Empty -> LocalContext.current.getString(R.string.widget_no_notes)
        is WidgetState.Error -> state.message
        is WidgetState.Success -> ""
    }

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurface)
        )
    }
}
