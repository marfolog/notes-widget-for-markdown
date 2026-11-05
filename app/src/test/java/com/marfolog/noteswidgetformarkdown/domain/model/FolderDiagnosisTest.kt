package com.marfolog.noteswidgetformarkdown.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderDiagnosisTest {

    private fun sub(name: String, count: Int) = SubfolderCount("id/$name", name, count)

    @Test
    fun `notes directly in the folder are found notes`() {
        val scan = FolderScan(readable = true, directNotes = 12, subfolders = listOf(sub("Daily", 30)))
        assertEquals(FolderDiagnosis.HasNotes(12), FolderDiagnosis.of(scan))
    }

    @Test
    fun `no notes directly but some below is the subfolder case, busiest first`() {
        val scan = FolderScan(
            readable = true,
            subfolders = listOf(sub("Projects", 12), sub("Archive", 0), sub("Daily", 30), sub("Recipes", 6))
        )
        val diagnosis = FolderDiagnosis.of(scan) as FolderDiagnosis.NotesInSubfolders
        assertEquals(48, diagnosis.total)
        assertEquals(listOf("Daily", "Projects", "Recipes"), diagnosis.subfolders.map { it.path })
    }

    @Test
    fun `equal counts are ordered by name`() {
        val scan = FolderScan(readable = true, subfolders = listOf(sub("beta", 2), sub("Alpha", 2)))
        val diagnosis = FolderDiagnosis.of(scan) as FolderDiagnosis.NotesInSubfolders
        assertEquals(listOf("Alpha", "beta"), diagnosis.subfolders.map { it.path })
    }

    @Test
    fun `the list on screen holds at most twenty folders but the total counts them all`() {
        val scan = FolderScan(readable = true, subfolders = (1..30).map { sub("Folder $it", it) })
        val diagnosis = FolderDiagnosis.of(scan) as FolderDiagnosis.NotesInSubfolders
        assertEquals(30, diagnosis.subfolders.size)
        assertEquals(20, diagnosis.listed.size)
        assertEquals("Folder 30", diagnosis.listed.first().path)
        assertEquals((1..30).sum(), diagnosis.total)
    }

    @Test
    fun `the summary names the busiest folders with their paths and a dots when there are more`() {
        val scan = FolderScan(
            readable = true,
            subfolders = listOf(sub("Daily", 30), sub("Projects/Work", 5), sub("Recipes", 6))
        )
        val diagnosis = FolderDiagnosis.of(scan) as FolderDiagnosis.NotesInSubfolders
        assertEquals("Daily (30), Recipes (6), Projects/Work (5)", diagnosis.summary())
        assertEquals("Daily (30), …", diagnosis.summary(limit = 1))
    }

    @Test
    fun `a search that stopped early says the count is a lower bound`() {
        val scan = FolderScan(readable = true, subfolders = listOf(sub("Daily", 1_900)), truncated = true)
        assertTrue((FolderDiagnosis.of(scan) as FolderDiagnosis.NotesInSubfolders).truncated)
    }

    @Test
    fun `other files and no markdown anywhere`() {
        val scan = FolderScan(readable = true, otherFiles = 4, subfolders = listOf(sub("Photos", 0)))
        assertEquals(FolderDiagnosis.NoMarkdown(4), FolderDiagnosis.of(scan))
    }

    @Test
    fun `nothing at all is an empty folder`() {
        assertEquals(FolderDiagnosis.EmptyFolder, FolderDiagnosis.of(FolderScan(readable = true)))
    }

    @Test
    fun `empty subfolders alone still make an empty folder`() {
        val scan = FolderScan(readable = true, subfolders = listOf(sub("Daily", 0)))
        assertEquals(FolderDiagnosis.EmptyFolder, FolderDiagnosis.of(scan))
    }

    @Test
    fun `a folder that cannot be read or was not scanned is no access`() {
        assertEquals(FolderDiagnosis.NoAccess, FolderDiagnosis.of(FolderScan.Unreadable))
        assertEquals(FolderDiagnosis.NoAccess, FolderDiagnosis.of(null))
    }

    @Test
    fun `the analytics value is a fixed word, never a name`() {
        assertEquals("has_notes", FolderDiagnosis.HasNotes(1).eventValue)
        assertEquals("in_subfolders", FolderDiagnosis.NotesInSubfolders(1, emptyList(), false).eventValue)
        assertEquals("no_markdown", FolderDiagnosis.NoMarkdown(1).eventValue)
        assertEquals("empty", FolderDiagnosis.EmptyFolder.eventValue)
        assertEquals("no_access", FolderDiagnosis.NoAccess.eventValue)
    }
}
