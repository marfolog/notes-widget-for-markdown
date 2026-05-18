package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.data.preferences.NoteCardSettingsStore
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardAppearance
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardColor
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.usecase.GetNotesUseCase
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import kotlinx.coroutines.flow.firstOrNull
import org.koin.java.KoinJavaComponent.get

class NotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val folderUri = prefs.getString(SetupActivity.KEY_NOTES_URI, null)
            ?: prefs.getString(SetupActivity.KEY_VAULT_URI, null)

        val vaultName = prefs.getString(SetupActivity.KEY_VAULT_NAME, null)
        val noteFolderPath = prefs.getString(SetupActivity.KEY_NOTE_FOLDER_PATH, null)
        val cardSettings = NoteCardSettingsStore(context).getAll()

        val state: WidgetState = if (folderUri.isNullOrEmpty()) {
            WidgetState.Uninitialized
        } else {
            runCatching {
                val getNotesUseCase = get<GetNotesUseCase>(GetNotesUseCase::class.java)
                val notes = getNotesUseCase(folderUri).firstOrNull() ?: emptyList()
                if (notes.isEmpty()) {
                    WidgetState.Empty
                } else {
                    WidgetState.Success(notes)
                }
            }.getOrElse { e ->
                val message = e.message ?: "Unknown error"
                if (message.contains("permission", ignoreCase = true) ||
                    e is SecurityException
                ) {
                    WidgetState.PermissionLost
                } else {
                    WidgetState.Error(message)
                }
            }
        }

        provideContent {
            GlanceTheme {
                WidgetRoot(state, vaultName, noteFolderPath, cardSettings)
            }
        }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(NotesWidget::class.java)
            ids.forEach { id ->
                NotesWidget().update(context, id)
            }
        }
    }
}

private sealed interface WidgetState {
    data object Uninitialized : WidgetState
    data object PermissionLost : WidgetState
    data class Success(val notes: List<NoteSummary>) : WidgetState
    data object Empty : WidgetState
    data class Error(val message: String) : WidgetState
}

// region Root

@Composable
private fun WidgetRoot(
    state: WidgetState,
    vaultName: String?,
    noteFolderPath: String?,
    cardSettings: Map<String, NoteCardAppearance>
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
            is WidgetState.Success -> ConfiguredContent(state.notes, vaultName, noteFolderPath, cardSettings)
            is WidgetState.Empty -> ConfiguredContentEmpty(vaultName, noteFolderPath)
            is WidgetState.Error -> ConfiguredContentError(state.message, vaultName, noteFolderPath)
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
            contentDescription = "Setup",
            modifier = GlanceModifier.size(48.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
        )
        Spacer(modifier = GlanceModifier.height(16.dp))
        Text(
            text = "Setup Required",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "Tap to select your notes folder",
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
            contentDescription = "Error",
            modifier = GlanceModifier.size(48.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.error)
        )
        Spacer(modifier = GlanceModifier.height(16.dp))
        Text(
            text = "Permission lost",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = GlanceTheme.colors.error
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "Tap to re-select your folder",
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
    cardSettings: Map<String, NoteCardAppearance>
) {
    Box(modifier = GlanceModifier.fillMaxSize()) {
        NotesList(notes, vaultName, cardSettings)
        WidgetActionButtons()
        Fab(vaultName, noteFolderPath)
    }
}

@Composable
private fun ConfiguredContentEmpty(vaultName: String?, noteFolderPath: String?) {
    Box(modifier = GlanceModifier.fillMaxSize()) {
        EmptyContent()
        WidgetActionButtons()
        Fab(vaultName, noteFolderPath)
    }
}

@Composable
private fun ConfiguredContentError(message: String, vaultName: String?, noteFolderPath: String?) {
    Box(modifier = GlanceModifier.fillMaxSize()) {
        ErrorContent(message)
        WidgetActionButtons()
        Fab(vaultName, noteFolderPath)
    }
}

// endregion

// region Widget Action Buttons (Top-Right)

@Composable
private fun WidgetActionButtons() {
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
                contentDescription = "Sync notes",
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
private fun RoundIconButton(
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

// region FAB (Bottom-Left)

@Composable
private fun Fab(vaultName: String?, noteFolderPath: String?) {
    val noteName = "New Note"
    val filePath = if (!noteFolderPath.isNullOrEmpty()) {
        "$noteFolderPath/$noteName"
    } else {
        noteName
    }
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
        modifier = GlanceModifier.fillMaxSize().padding(4.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Box(
            modifier = GlanceModifier
                .size(56.dp)
                .cornerRadius(16.dp)
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
    }
}

// endregion

// region Notes List

@Composable
private fun NotesList(
    notes: List<NoteSummary>,
    vaultName: String?,
    cardSettings: Map<String, NoteCardAppearance>
) {
    LazyColumn(
        modifier = GlanceModifier
            .fillMaxSize()
    ) {
        items(notes, itemId = { it.id.hashCode().toLong() }) { note ->
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                NoteCard(note, vaultName, cardSettings[note.fileUri] ?: NoteCardAppearance())
                Spacer(modifier = GlanceModifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun NoteCard(note: NoteSummary, vaultName: String?, appearance: NoteCardAppearance) {
    val obsidianUri = buildString {
        append("obsidian://open?")
        if (!vaultName.isNullOrEmpty()) {
            append("vault=${Uri.encode(vaultName)}&")
        }
        append("file=${Uri.encode(note.fileName)}")
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

@Composable
private fun NoteCardAppearance.backgroundColor(): ColorProvider {
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
private fun NoteCardAppearance.contentColor(): ColorProvider {
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

private fun parseCustomColor(value: String?): Color? {
    val normalized = value?.trim().orEmpty()
    if (!Regex("^#?[0-9A-Fa-f]{6}$").matches(normalized)) {
        return null
    }
    val hex = normalized.removePrefix("#")
    return Color(("FF$hex").toLong(16))
}

private fun customContentColor(value: String?): Color {
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

// region Empty & Error Content

@Composable
private fun EmptyContent(modifier: GlanceModifier = GlanceModifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No notes found",
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
                contentDescription = "Error",
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
