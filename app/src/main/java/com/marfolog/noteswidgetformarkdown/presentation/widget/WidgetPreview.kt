package com.marfolog.noteswidgetformarkdown.presentation.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    ),
    NoteSummary(
        id = "6",
        title = "Recipes",
        preview = "Pasta aglio e olio\nGarlic, chili flakes, parsley\nCook pasta al dente",
        lastModified = 1708358400000,
        fileUri = "",
        fileName = "Recipes.md"
    )
)

@Composable
private fun WidgetPreviewContent(notes: List<NoteSummary>) {
    Column(modifier = Modifier.fillMaxSize()) {
        WidgetHeaderPreview()
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(notes) { note ->
                NoteCardPreview(note)
            }
        }
    }
}

@Composable
private fun WidgetHeaderPreview() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Notes",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "↻",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun NoteCardPreview(note: NoteSummary) {
    Surface(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
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
    name = "Widget - Notes Grid",
    widthDp = 360,
    heightDp = 400
)
@Composable
private fun WidgetNotesGridPreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        ) {
            WidgetPreviewContent(sampleNotes)
        }
    }
}

@Preview(
    showBackground = true,
    name = "Widget - Dark - Notes Grid",
    widthDp = 360,
    heightDp = 400
)
@Composable
private fun WidgetNotesGridDarkPreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false, darkTheme = true) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        ) {
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
    widthDp = 180
)
@Composable
private fun NoteCardSinglePreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            NoteCardPreview(sampleNotes.first())
        }
    }
}

@Preview(
    showBackground = true,
    name = "NoteCard - Dark - Single",
    widthDp = 180
)
@Composable
private fun NoteCardSingleDarkPreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false, darkTheme = true) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            NoteCardPreview(sampleNotes.first())
        }
    }
}
