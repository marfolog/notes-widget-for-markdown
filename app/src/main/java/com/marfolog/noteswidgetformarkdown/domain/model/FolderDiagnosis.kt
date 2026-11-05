package com.marfolog.noteswidgetformarkdown.domain.model

/**
 * A folder below the notes folder with Markdown files directly in it, at any depth. [path] is
 * relative to the notes folder ("Projects/Work"), which is what tells two folders named alike apart.
 */
data class SubfolderCount(val documentId: String, val path: String, val count: Int)

/**
 * What a look inside the notes folder found. Only counted, never read: the widget still shows the
 * files that sit directly in the folder, this exists to tell the user why there are none.
 */
data class FolderScan(
    val readable: Boolean,
    /** Markdown files directly in the folder, without the folder note Obsidian hides. */
    val directNotes: Int = 0,
    /** Files that are not Markdown, anywhere in the part of the tree that was searched. */
    val otherFiles: Int = 0,
    val subfolders: List<SubfolderCount> = emptyList(),
    /** The search stopped at its limits, so the counts are at least what they say. */
    val truncated: Boolean = false
) {
    companion object {
        val Unreadable = FolderScan(readable = false)
    }
}

/** Why a folder shows what it shows. Decided by [FolderDiagnosis.of], so it can be tested without a device. */
sealed interface FolderDiagnosis {
    data class HasNotes(val count: Int) : FolderDiagnosis

    /**
     * [total] counts the notes in all [subfolders], busiest first; [truncated] marks a lower bound.
     * Only folders with notes of their own are in the list.
     */
    data class NotesInSubfolders(
        val total: Int,
        val subfolders: List<SubfolderCount>,
        val truncated: Boolean
    ) : FolderDiagnosis {
        /** What a list on screen shows; nobody scrolls through hundreds of folders to pick one. */
        val listed: List<SubfolderCount> get() = subfolders.take(MAX_LISTED)
    }

    data class NoMarkdown(val otherFiles: Int) : FolderDiagnosis
    data object EmptyFolder : FolderDiagnosis
    data object NoAccess : FolderDiagnosis

    /** The value of the `result` parameter of `folder_selected`. */
    val eventValue: String
        get() = when (this) {
            is HasNotes -> "has_notes"
            is NotesInSubfolders -> "in_subfolders"
            is NoMarkdown -> "no_markdown"
            EmptyFolder -> "empty"
            NoAccess -> "no_access"
        }

    companion object {
        const val MAX_LISTED = 20

        fun of(scan: FolderScan?): FolderDiagnosis {
            if (scan == null || !scan.readable) return NoAccess
            if (scan.directNotes > 0) return HasNotes(scan.directNotes)
            val withNotes = scan.subfolders
                .filter { it.count > 0 }
                .sortedWith(compareByDescending<SubfolderCount> { it.count }.thenBy { it.path.lowercase() })
            if (withNotes.isNotEmpty()) {
                return NotesInSubfolders(withNotes.sumOf { it.count }, withNotes, scan.truncated)
            }
            return if (scan.otherFiles > 0) NoMarkdown(scan.otherFiles) else EmptyFolder
        }
    }
}

/** "Daily (30), Projects/Work (5), …": the busiest folders in a line, with "…" when there are more. */
fun FolderDiagnosis.NotesInSubfolders.summary(limit: Int = 4): String {
    val line = subfolders.take(limit).joinToString(", ") { "${it.path} (${it.count})" }
    return if (subfolders.size > limit || truncated) "$line, …" else line
}
