package com.marfolog.noteswidgetformarkdown.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Deep links into Obsidian are built from the vault name plus this path. When it came out wrong,
 * every tap opened whichever note Obsidian had open last instead of the one that was tapped —
 * a bug that survived for weeks because nothing here was covered.
 */
class ObsidianVaultLocatorTest {

    private val vault = "primary:Documents/my-vault"

    @Test
    fun `the notes folder being the vault root gives an empty path`() {
        assertEquals("", relative(vault))
    }

    @Test
    fun `a subfolder gives its path relative to the vault`() {
        assertEquals("Projects", relative("$vault/Projects"))
    }

    @Test
    fun `nesting deeper keeps the whole path`() {
        assertEquals("Projects/2026/Q3", relative("$vault/Projects/2026/Q3"))
    }

    @Test
    fun `a folder outside the vault has no relative path`() {
        assertNull(relative("primary:Documents/other-vault/Projects"))
    }

    /**
     * The bug that made this worth extracting: a prefix match without the separator accepts
     * `my-vault-backup` as living inside `my-vault`, and the resulting path is nonsense.
     */
    @Test
    fun `a sibling folder whose name merely starts the same is not inside the vault`() {
        assertNull(relative("primary:Documents/my-vault-backup"))
        assertNull(relative("primary:Documents/my-vault-backup/Projects"))
    }

    @Test
    fun `the vault being inside the notes folder is not a match either`() {
        assertNull(relative("primary:Documents"))
    }

    @Test
    fun `a trailing separator is not mistaken for a subfolder`() {
        assertEquals("", relative("$vault/").orEmpty())
    }

    private fun relative(notesDocumentId: String): String? =
        ObsidianVaultLocator.relativeDocumentPath(vault, notesDocumentId)
}
