package com.marfolog.noteswidgetformarkdown.presentation.widget

import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary

/** What the fast note dialog shows. */
internal sealed interface FastNoteStep {
    data object Write : FastNoteStep
    data object Loading : FastNoteStep

    /** [notes] may be empty: the app's own file can be created even in a folder without notes. */
    data class PickTarget(val notes: List<NoteSummary>) : FastNoteStep

    /** A file with content was picked; new notes go at its end, which is said once before it is taken. */
    data class ConfirmTarget(val note: NoteSummary) : FastNoteStep
    data object NoNotesFolder : FastNoteStep
    data object NotesUnreadable : FastNoteStep
}

/**
 * Without a usable target the dialog asks for one right there, from the same notes the settings
 * offer, instead of sending the user off to settings first. A saved target that no longer exists
 * or cannot be written counts as no target, and so does one the user cleared. [targetExists] and
 * [folderNotes] are null while still loading; [confirming] is the file picked and not yet taken.
 */
internal fun fastNoteStep(
    targetUri: String?,
    targetExists: Boolean?,
    notesFolderUri: String?,
    folderNotes: Result<List<NoteSummary>>?,
    confirming: NoteSummary? = null
): FastNoteStep {
    if (!targetUri.isNullOrBlank()) {
        if (targetExists == null) return FastNoteStep.Loading
        if (targetExists) return FastNoteStep.Write
    }
    if (notesFolderUri.isNullOrBlank()) return FastNoteStep.NoNotesFolder
    if (folderNotes == null) return FastNoteStep.Loading
    val notes = folderNotes.getOrNull() ?: return FastNoteStep.NotesUnreadable
    if (confirming != null) return FastNoteStep.ConfirmTarget(confirming)
    return FastNoteStep.PickTarget(notes.filterNot { it.isExtra })
}
