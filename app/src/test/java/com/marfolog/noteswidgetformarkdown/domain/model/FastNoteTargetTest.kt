package com.marfolog.noteswidgetformarkdown.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FastNoteTargetTest {

    @Test
    fun `a free name is used as is`() {
        assertEquals("Quick notes.md", FastNoteTarget.freeFileName("Quick notes", emptyList()))
        assertEquals("Quick notes.md", FastNoteTarget.freeFileName("Quick notes", listOf("Plan.md", "Groceries.md")))
    }

    @Test
    fun `a taken name gets the next number`() {
        assertEquals("Quick notes 2.md", FastNoteTarget.freeFileName("Quick notes", listOf("Quick notes.md")))
        assertEquals(
            "Quick notes 3.md",
            FastNoteTarget.freeFileName("Quick notes", listOf("Quick notes.md", "Quick notes 2.md"))
        )
    }

    @Test
    fun `a gap in the numbers is filled`() {
        assertEquals(
            "Quick notes 2.md",
            FastNoteTarget.freeFileName("Quick notes", listOf("Quick notes.md", "Quick notes 3.md"))
        )
    }

    @Test
    fun `case does not make a name free`() {
        assertEquals("Quick notes 2.md", FastNoteTarget.freeFileName("Quick notes", listOf("QUICK NOTES.md")))
    }

    @Test
    fun `a translated name works the same`() {
        assertEquals(
            "Rychlé poznámky 2.md",
            FastNoteTarget.freeFileName("Rychlé poznámky", listOf("Rychlé poznámky.md"))
        )
    }

    @Test
    fun `a hundred taken names still end in a free one`() {
        val taken = listOf("Quick notes.md") + (2..100).map { "Quick notes $it.md" }
        assertEquals("Quick notes 101.md", FastNoteTarget.freeFileName("Quick notes", taken))
    }

    @Test
    fun `a file with content is warned about`() {
        assertTrue(FastNoteTarget.needsWarning(1_240))
        assertTrue(FastNoteTarget.needsWarning(1))
    }

    @Test
    fun `an empty file is taken without asking`() {
        assertFalse(FastNoteTarget.needsWarning(0))
    }

    @Test
    fun `a file that cannot be measured is warned about`() {
        assertTrue(FastNoteTarget.needsWarning(null))
    }

    private fun note(name: String, at: Long) = NoteSummary("id/$name", name, "", at, "uri/$name", "$name.md")

    @Test
    fun `chips list the newest change first`() {
        val ordered = FastNoteTarget.chipOrder(listOf(note("old", 1), note("new", 3), note("mid", 2)))
        assertEquals(listOf("old", "mid", "new").reversed(), ordered.map { it.title })
    }

    @Test
    fun `notes changed at the same time keep their order and an empty list stays empty`() {
        assertEquals(listOf("a", "b"), FastNoteTarget.chipOrder(listOf(note("a", 5), note("b", 5))).map { it.title })
        assertEquals(emptyList<NoteSummary>(), FastNoteTarget.chipOrder(emptyList()))
    }
}
