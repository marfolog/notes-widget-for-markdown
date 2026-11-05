package com.marfolog.noteswidgetformarkdown.data.repository

import com.marfolog.noteswidgetformarkdown.domain.model.FolderDiagnosis
import java.io.FileNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderScannerTest {

    /** A fake provider: folder id to children. A missing id means the folder cannot be read. */
    private class FakeTree : DirectoryLister {
        val children = mutableMapOf<String, List<DocEntry>>()
        val reads = mutableListOf<String>()
        var failWith: Exception? = null

        override fun list(directoryId: String): List<DocEntry>? {
            reads += directoryId
            failWith?.let { throw it }
            return children[directoryId]
        }

        fun dir(id: String, name: String) = DocEntry(id, name, isDirectory = true)
        fun file(id: String, name: String?) = DocEntry(id, name, isDirectory = false)
    }

    private fun scan(tree: FakeTree, limits: ScanLimits = ScanLimits(), clock: () -> Long = { 0L }) =
        FolderScanner.scan("root", "MyNotes", tree, limits, clock)

    @Test
    fun `an empty vault is an empty folder`() {
        val tree = FakeTree().apply { children["root"] = emptyList() }
        assertEquals(FolderDiagnosis.EmptyFolder, FolderDiagnosis.of(scan(tree)))
    }

    @Test
    fun `zero markdown files but other files`() {
        val tree = FakeTree().apply {
            children["root"] = listOf(file("a", "photo.jpg"), file("b", "todo.txt"))
        }
        assertEquals(FolderDiagnosis.NoMarkdown(2), FolderDiagnosis.of(scan(tree)))
    }

    @Test
    fun `markdown directly in the folder is counted, case of the extension does not matter`() {
        val tree = FakeTree().apply {
            children["root"] = listOf(file("a", "Groceries.md"), file("b", "Plan.MD"), file("c", "x.txt"))
        }
        assertEquals(FolderDiagnosis.HasNotes(2), FolderDiagnosis.of(scan(tree)))
    }

    @Test
    fun `notes only in subfolders are counted per folder that holds them, with its relative path`() {
        val tree = FakeTree().apply {
            children["root"] = listOf(dir("d", "Daily"), dir("p", "Projects"))
            children["d"] = listOf(file("d1", "2026-11-01.md"), file("d2", "2026-11-02.md"))
            children["p"] = listOf(dir("p/a", "Alpha"), file("p1", "Ideas.md"))
            children["p/a"] = listOf(file("pa1", "Spec.md"), file("pa2", "Notes.md"))
        }
        val diagnosis = FolderDiagnosis.of(scan(tree)) as FolderDiagnosis.NotesInSubfolders
        assertEquals(5, diagnosis.total)
        assertEquals(
            listOf("Daily" to 2, "Projects/Alpha" to 2, "Projects" to 1),
            diagnosis.subfolders.map { it.path to it.count }
        )
        assertEquals("d", diagnosis.subfolders.first().documentId)
        assertEquals("p/a", diagnosis.subfolders[1].documentId)
        assertFalse(diagnosis.truncated)
    }

    @Test
    fun `obsidian trash and git folders and any hidden folder are skipped`() {
        val tree = FakeTree().apply {
            children["root"] = listOf(
                dir(".obsidian", ".obsidian"), dir(".trash", ".trash"), dir(".git", ".git"),
                dir(".hidden", ".hidden"), dir("d", "Daily")
            )
            children[".obsidian"] = listOf(file("o", "workspace.md"))
            children[".trash"] = listOf(file("t", "Deleted.md"))
            children[".git"] = listOf(file("g", "README.md"))
            children[".hidden"] = listOf(file("h", "Secret.md"))
            children["d"] = listOf(file("d1", "Day.md"))
        }
        val diagnosis = FolderDiagnosis.of(scan(tree)) as FolderDiagnosis.NotesInSubfolders
        assertEquals(1, diagnosis.total)
        assertEquals(listOf("root", "d"), tree.reads)
    }

    @Test
    fun `a fresh vault with only the obsidian folder is empty`() {
        val tree = FakeTree().apply {
            children["root"] = listOf(dir(".obsidian", ".obsidian"))
            children[".obsidian"] = listOf(file("a", "app.json"))
        }
        assertEquals(FolderDiagnosis.EmptyFolder, FolderDiagnosis.of(scan(tree)))
    }

    @Test
    fun `the folder note is not counted, in the folder or below it`() {
        val tree = FakeTree().apply {
            children["root"] = listOf(file("a", "MyNotes.md"), dir("p", "Projects"))
            children["p"] = listOf(file("p1", "Projects.md"), file("p2", "Real.md"))
        }
        val scan = scan(tree)
        assertEquals(0, scan.directNotes)
        assertEquals(1, scan.subfolders.single().count)
    }

    @Test
    fun `a name the provider does not give is skipped, not a crash`() {
        val tree = FakeTree().apply {
            children["root"] = listOf(file("a", null), file("b", ""), DocEntry("c", null, true), file("d", "Real.md"))
        }
        assertEquals(FolderDiagnosis.HasNotes(1), FolderDiagnosis.of(scan(tree)))
    }

    @Test
    fun `three thousand files stop at the item limit and say so`() {
        val tree = FakeTree().apply {
            children["root"] = listOf(dir("d", "Big"))
            children["d"] = (1..3_000).map { file("f$it", "note $it.md") }
        }
        val result = scan(tree)
        assertTrue(result.truncated)
        assertEquals(1_999, result.subfolders.single().count)
        assertTrue((FolderDiagnosis.of(result) as FolderDiagnosis.NotesInSubfolders).truncated)
    }

    @Test
    fun `three thousand files directly in the folder are still just found notes`() {
        val tree = FakeTree().apply { children["root"] = (1..3_000).map { file("f$it", "note $it.md") } }
        val result = scan(tree)
        assertTrue(result.truncated)
        assertEquals(FolderDiagnosis.HasNotes(2_000), FolderDiagnosis.of(result))
    }

    @Test
    fun `a search that takes too long stops with what it has`() {
        val tree = FakeTree().apply {
            children["root"] = listOf(dir("a", "A"), dir("b", "B"))
            children["a"] = listOf(file("a1", "One.md"))
            children["b"] = listOf(file("b1", "Two.md"))
        }
        var now = 0L
        // Every look at the clock costs a second, so the second folder is already too late.
        val result = scan(tree, ScanLimits(maxMillis = 1_500)) { now += 1_000; now }
        assertTrue(result.truncated)
        assertTrue(result.readable)
        assertTrue(tree.reads.size < 3)
    }

    @Test
    fun `folders nested deeper than four levels are not entered`() {
        val tree = FakeTree().apply {
            children["root"] = listOf(dir("1", "L1"))
            children["1"] = listOf(dir("2", "L2"))
            children["2"] = listOf(dir("3", "L3"))
            children["3"] = listOf(dir("4", "L4"))
            children["4"] = listOf(dir("5", "L5"), file("x", "Four.md"))
            children["5"] = listOf(file("y", "Five.md"))
        }
        val result = scan(tree)
        assertEquals(1, result.subfolders.single().count)
        assertFalse("5" in tree.reads)
    }

    @Test
    fun `a link back to a folder already seen does not loop`() {
        val tree = FakeTree().apply {
            children["root"] = listOf(dir("a", "A"))
            children["a"] = listOf(dir("root", "Back"), dir("a", "Self"), file("n", "Note.md"))
        }
        val result = scan(tree)
        assertEquals(1, result.subfolders.single().count)
        assertEquals(listOf("root", "a"), tree.reads)
    }

    @Test
    fun `a cycle of ever new ids ends at the depth limit`() {
        val tree = object : DirectoryLister {
            var reads = 0
            override fun list(directoryId: String): List<DocEntry> {
                reads++
                return listOf(DocEntry("$directoryId/link", "link", true), DocEntry("$directoryId/n", "n.md", false))
            }
        }
        val result = FolderScanner.scan("root", "MyNotes", tree)
        assertEquals(5, tree.reads)
        assertTrue(result.readable)
    }

    @Test
    fun `a root that cannot be read is no access`() {
        val tree = FakeTree()
        assertEquals(FolderDiagnosis.NoAccess, FolderDiagnosis.of(scan(tree)))
    }

    @Test
    fun `a revoked grant that throws is no access, whatever it throws`() {
        listOf(
            SecurityException("Permission Denial"),
            FileNotFoundException("gone"),
            IllegalArgumentException("Unknown URI"),
            IllegalStateException("provider died"),
            RuntimeException("anything")
        ).forEach { error ->
            val tree = FakeTree().apply { failWith = error }
            assertEquals(error.toString(), FolderDiagnosis.NoAccess, FolderDiagnosis.of(scan(tree)))
        }
    }

    @Test
    fun `a subfolder deleted in the middle of the search is skipped`() {
        val tree = FakeTree().apply {
            children["root"] = listOf(dir("gone", "Gone"), dir("d", "Daily"))
            children["d"] = listOf(file("d1", "Day.md"))
        }
        val diagnosis = FolderDiagnosis.of(scan(tree)) as FolderDiagnosis.NotesInSubfolders
        assertEquals(listOf("Daily"), diagnosis.subfolders.map { it.path })
    }

    @Test
    fun `a subfolder that throws is skipped and the rest still counts`() {
        val tree = object : DirectoryLister {
            override fun list(directoryId: String): List<DocEntry>? = when (directoryId) {
                "root" -> listOf(DocEntry("bad", "Bad", true), DocEntry("d", "Daily", true))
                "d" -> listOf(DocEntry("d1", "Day.md", false))
                else -> throw SecurityException("lost")
            }
        }
        val result = FolderScanner.scan("root", "MyNotes", tree)
        assertEquals(1, result.subfolders.single { it.count > 0 }.count)
        // What was in the folder that could not be read is unknown, so the counts are a lower bound.
        assertTrue(result.truncated)
    }

    @Test
    fun `a folder with only subfolders in it is not listed itself`() {
        val tree = FakeTree().apply {
            children["root"] = listOf(dir("p", "Projects"))
            children["p"] = listOf(dir("w", "Work"))
            children["w"] = listOf(file("w1", "A.md"), file("w2", "B.md"))
        }
        val diagnosis = FolderDiagnosis.of(scan(tree)) as FolderDiagnosis.NotesInSubfolders
        assertEquals(listOf("Projects/Work" to 2), diagnosis.subfolders.map { it.path to it.count })
    }

    @Test
    fun `two folders with the same name are told apart by their path`() {
        val tree = FakeTree().apply {
            children["root"] = listOf(dir("a", "Work"), dir("p", "Projects"))
            children["a"] = listOf(file("a1", "One.md"))
            children["p"] = listOf(dir("pw", "Work"))
            children["pw"] = listOf(file("p1", "Two.md"), file("p2", "Three.md"))
        }
        val diagnosis = FolderDiagnosis.of(scan(tree)) as FolderDiagnosis.NotesInSubfolders
        assertEquals(listOf("Projects/Work", "Work"), diagnosis.subfolders.map { it.path })
    }

    @Test
    fun `hidden and folder note rules`() {
        assertTrue(FolderScanner.isHidden(".obsidian"))
        assertFalse(FolderScanner.isHidden("Daily"))
        assertTrue(FolderScanner.isFolderNote("Projects.md", "projects"))
        assertFalse(FolderScanner.isFolderNote("Projects.md", null))
        assertFalse(FolderScanner.isFolderNote("Other.md", "Projects"))
    }
}
