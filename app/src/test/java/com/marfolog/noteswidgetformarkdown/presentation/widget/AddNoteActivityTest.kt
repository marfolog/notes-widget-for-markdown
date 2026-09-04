package com.marfolog.noteswidgetformarkdown.presentation.widget

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `nextAvailableTitle` only decides what name to *ask* the SAF provider for — it does not decide
 * what actually gets created. A stale `listFiles()` snapshot can miss a file that exists on disk,
 * so the provider is always the source of truth for the name it actually used. These tests only
 * cover the suggestion; the bug this guards against — opening the wrong file when the suggestion
 * collided — was fixed by having the caller open the uri the provider returned instead of
 * re-deriving a path from this title. See AddNoteActivity.openNewNote.
 */
class AddNoteActivityTest {

    @Test
    fun `an empty folder gets the plain base name`() {
        assertEquals("New Note", nextAvailableTitle("New Note", emptySet()))
    }

    @Test
    fun `one collision moves to the first numbered name`() {
        val existing = setOf("New Note.md")
        assertEquals("New Note 2", nextAvailableTitle("New Note", existing))
    }

    @Test
    fun `several collisions in a row are skipped`() {
        val existing = setOf("New Note.md", "New Note 2.md", "New Note 3.md")
        assertEquals("New Note 4", nextAvailableTitle("New Note", existing))
    }

    @Test
    fun `a gap in the numbering is still skipped over, not reused`() {
        // "New Note 2.md" is missing (maybe it was deleted), but 3 and 4 exist — the result must
        // not silently reuse 2, since another process could recreate it at any moment.
        val existing = setOf("New Note.md", "New Note 3.md", "New Note 4.md")
        assertEquals("New Note 2", nextAvailableTitle("New Note", existing))
    }

    @Test
    fun `a name that merely starts the same is not a collision`() {
        // "New Note Ideas.md" must not be mistaken for "New Note.md".
        val existing = setOf("New Note Ideas.md", "New Notebook.md")
        assertEquals("New Note", nextAvailableTitle("New Note", existing))
    }

    @Test
    fun `works with a translated base name, diacritics and all`() {
        val existing = setOf("Nová poznámka.md")
        assertEquals("Nová poznámka 2", nextAvailableTitle("Nová poznámka", existing))
    }
}
