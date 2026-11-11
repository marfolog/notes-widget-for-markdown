package com.marfolog.noteswidgetformarkdown.presentation.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StarterFolderTest {

    @Test
    fun `the example note is made in Documents Notes with a Markdown name`() {
        assertEquals("Documents/Notes/", StarterFolder.RELATIVE_PATH)
        assertTrue(StarterFolder.FILE_NAME.endsWith(".md"))
    }

    @Test
    fun `a Notes folder that already exists gets no example note, as it may hold the user's own`() {
        assertFalse(StarterFolder.shouldCreateNote(folderExists = true))
        assertTrue(StarterFolder.shouldCreateNote(folderExists = false))
    }

    @Test
    fun `a name the media store renamed to a numbered copy counts as a duplicate`() {
        assertFalse(StarterFolder.isDuplicate("Welcome to Notes Widget.md", "Welcome to Notes Widget.md"))
        assertTrue(StarterFolder.isDuplicate("Welcome to Notes Widget.md", "Welcome to Notes Widget (1).md"))
        assertTrue(StarterFolder.isDuplicate("Welcome to Notes Widget.md", null))
    }
}
