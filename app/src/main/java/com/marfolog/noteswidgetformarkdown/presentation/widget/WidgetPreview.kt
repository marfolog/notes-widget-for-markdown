package com.marfolog.noteswidgetformarkdown.presentation.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.ui.theme.NotesWidgetForMarkdownTheme

private val sampleNotes = listOf(
    NoteSummary(
        id = "1",
        title = "Meeting Notes",
        preview = "Discussed Q3 roadmap\nAction items for the team\nFollow up next Tuesday",
        lastModified = 1708790400000,
        fileUri = "",
        fileName = "Meeting Notes.md"
    ),
    NoteSummary(
        id = "2",
        title = "Shopping List",
        preview = "Milk, eggs, bread\nCoffee beans\nOlive oil",
        lastModified = 1708704000000,
        fileUri = "",
        fileName = "Shopping List.md"
    ),
    NoteSummary(
        id = "3",
        title = "Project Ideas",
        preview = "Weather dashboard app\nMarkdown widget for Android\nRecipe organizer",
        lastModified = 1708617600000,
        fileUri = "",
        fileName = "Project Ideas.md"
    ),
    NoteSummary(
        id = "4",
        title = "Daily Journal",
        preview = "Today was productive\nFinished the widget layout\nNeed to review PR tomorrow",
        lastModified = 1708531200000,
        fileUri = "",
        fileName = "Daily Journal.md"
    ),
    NoteSummary(
        id = "5",
        title = "Book Notes",
        preview = "Atomic Habits key takeaways\n1% better every day\nHabit stacking technique",
        lastModified = 1708444800000,
        fileUri = "",
        fileName = "Book Notes.md"
    )
)

@Composable
private fun WidgetPreviewContent(notes: List<NoteSummary>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Notes list
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            notes.forEachIndexed { index, note ->
                NoteCardPreview(note)
                if (index < notes.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Refresh button — top end
        IconButton(
            onClick = {},
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // FAB — bottom start
        FloatingActionButton(
            onClick = {},
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(56.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add note",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun NoteCardPreview(note: NoteSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = note.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.preview,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyStatePreview(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// --- Previews ---

@Preview(
    showBackground = true,
    name = "Widget - Notes List",
    widthDp = 360,
    heightDp = 400
)
@Composable
private fun WidgetNotesPreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false) {
        Surface(shape = RoundedCornerShape(24.dp)) {
            WidgetPreviewContent(sampleNotes)
        }
    }
}

@Preview(
    showBackground = true,
    name = "Widget - Dark - Notes List",
    widthDp = 360,
    heightDp = 400
)
@Composable
private fun WidgetNotesDarkPreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false, darkTheme = true) {
        Surface(shape = RoundedCornerShape(24.dp)) {
            WidgetPreviewContent(sampleNotes)
        }
    }
}

@Preview(
    showBackground = true,
    name = "Widget - Empty State",
    widthDp = 360,
    heightDp = 200
)
@Composable
private fun WidgetEmptyStatePreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        ) {
            EmptyStatePreview("No notes found")
        }
    }
}

@Preview(
    showBackground = true,
    name = "Widget - No Folder",
    widthDp = 360,
    heightDp = 200
)
@Composable
private fun WidgetNoFolderPreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        ) {
            EmptyStatePreview("Tap to select a folder")
        }
    }
}

@Preview(
    showBackground = true,
    name = "NoteCard - Single",
    widthDp = 360
)
@Composable
private fun NoteCardSinglePreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
        ) {
            NoteCardPreview(sampleNotes.first())
        }
    }
}

@Preview(
    showBackground = true,
    name = "NoteCard - Dark - Single",
    widthDp = 360
)
@Composable
private fun NoteCardSingleDarkPreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false, darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
        ) {
            NoteCardPreview(sampleNotes.first())
        }
    }
}
