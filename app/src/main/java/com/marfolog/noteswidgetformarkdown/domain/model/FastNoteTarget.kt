package com.marfolog.noteswidgetformarkdown.domain.model

/** The file the app creates for fast notes, so nobody has to write into a file of their own. */
data class CreatedFile(val uri: String, val name: String)

/** Decisions about the fast note target that need no device, so they can be tested. */
object FastNoteTarget {

    /** Newest change first, as the widget lists them; notes changed at the same time keep their order. */
    fun chipOrder(notes: List<NoteSummary>): List<NoteSummary> = notes.sortedByDescending { it.lastModified }

    /**
     * "Quick notes.md", or the first of "Quick notes 2.md", "Quick notes 3.md"… that is free.
     * File systems compare names without caring about case, so this does too.
     */
    fun freeFileName(baseName: String, existingNames: Collection<String>): String {
        val taken = existingNames.mapTo(HashSet()) { it.lowercase() }
        var number = 1
        while (true) {
            val candidate = if (number == 1) "$baseName.md" else "$baseName $number.md"
            if (candidate.lowercase() !in taken) return candidate
            number++
        }
    }

    /**
     * Whether to say that new notes go at the end before taking this file. An empty file is
     * harmless; one that cannot be measured is treated as not empty, since asking costs a tap and
     * a wrong guess costs lines in someone's plan.
     */
    fun needsWarning(sizeBytes: Long?): Boolean = sizeBytes == null || sizeBytes > 0
}
