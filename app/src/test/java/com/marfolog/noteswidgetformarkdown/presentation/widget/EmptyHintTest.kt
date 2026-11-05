package com.marfolog.noteswidgetformarkdown.presentation.widget

import com.marfolog.noteswidgetformarkdown.domain.model.FolderDiagnosis
import com.marfolog.noteswidgetformarkdown.domain.model.SubfolderCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmptyHintTest {

    @Test
    fun `notes in subfolders say so and lead to settings`() {
        val diagnosis = FolderDiagnosis.NotesInSubfolders(30, listOf(SubfolderCount("d", "Daily", 30)), false)
        val hint = emptyHintFor(diagnosis)
        assertEquals(EmptyHint.NotesInSubfolders, hint)
        assertTrue(hint.opensSettings)
    }

    @Test
    fun `a folder without markdown leads to settings too`() {
        val hint = emptyHintFor(FolderDiagnosis.NoMarkdown(3))
        assertEquals(EmptyHint.NoMarkdown, hint)
        assertTrue(hint.opensSettings)
    }

    @Test
    fun `an empty folder only says so, the plus button is right there`() {
        val hint = emptyHintFor(FolderDiagnosis.EmptyFolder)
        assertEquals(EmptyHint.EmptyFolder, hint)
        assertFalse(hint.opensSettings)
    }

    @Test
    fun `no access and a list that contradicts the folder keep the plain message`() {
        assertEquals(EmptyHint.None, emptyHintFor(FolderDiagnosis.NoAccess))
        assertEquals(EmptyHint.None, emptyHintFor(FolderDiagnosis.HasNotes(2)))
        assertFalse(EmptyHint.None.opensSettings)
    }

    @Test
    fun `every hint has its own message`() {
        val messages = EmptyHint.entries.map { emptyMessageRes(it) }
        assertEquals(messages.size, messages.toSet().size)
    }
}
