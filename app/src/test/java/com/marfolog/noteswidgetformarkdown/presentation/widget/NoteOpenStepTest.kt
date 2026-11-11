package com.marfolog.noteswidgetformarkdown.presentation.widget

import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteOpenStepTest {

    private fun note(isExtra: Boolean = false) = NoteSummary(
        id = "welcome",
        fileName = "Welcome to Notes Widget.md",
        title = "Welcome to Notes Widget",
        preview = "",
        lastModified = 0L,
        fileUri = "content://com.android.externalstorage.documents/document/primary%3ADocuments%2FNotes%2FWelcome.md",
        isExtra = isExtra
    )

    @Test
    fun `the default setting tries Obsidian's link first`() {
        assertEquals(OpenStep.ObsidianLink, openStepFor(OpenWith.Obsidian, note(), noteFolderPath = null))
    }

    @Test
    fun `an extra note outside any vault goes to the file even with Obsidian chosen`() {
        assertEquals(OpenStep.File, openStepFor(OpenWith.Obsidian, note(isExtra = true), noteFolderPath = null))
    }

    @Test
    fun `the other settings open the file, or ask every time`() {
        assertEquals(OpenStep.File, openStepFor(OpenWith.DefaultApp, note(), "Notes"))
        assertEquals(OpenStep.FileChooser, openStepFor(OpenWith.AskEveryTime, note(), "Notes"))
    }

    @Test
    fun `without Obsidian on the phone the file itself is tried next`() {
        assertEquals(OpenStep.File, nextOpenStep(OpenStep.ObsidianLink))
    }

    @Test
    fun `when no app takes the file either nothing is left and the user is told`() {
        assertNull(nextOpenStep(OpenStep.File))
        assertNull(nextOpenStep(OpenStep.FileChooser))
    }
}
