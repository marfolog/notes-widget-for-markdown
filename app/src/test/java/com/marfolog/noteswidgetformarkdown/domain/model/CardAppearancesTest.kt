package com.marfolog.noteswidgetformarkdown.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardAppearancesTest {

    private val groceries = "content://notes/Groceries.md"
    private val projects = "content://notes/Projects.md"
    private val ownLook = NoteCardAppearance(size = NoteCardSize.Compact, color = NoteCardColor.Default)
    private val widgetDefault = NoteCardAppearance(size = NoteCardSize.Large, color = NoteCardColor.Mint, textSize = NoteCardTextSize.Large)

    @Test
    fun `nothing set anywhere is the compact card the widget always had`() {
        assertEquals(NoteCardAppearance(), CardAppearances().of(groceries))
        assertEquals(NoteCardSize.Compact, CardAppearances().of(groceries).size)
    }

    @Test
    fun `a card without its own look takes the widget default`() {
        val appearances = CardAppearances(own = mapOf(projects to ownLook), default = widgetDefault)

        assertEquals(widgetDefault, appearances.of(groceries))
    }

    @Test
    fun `a card set up on its own keeps its whole look, even where it matches the old default`() {
        val appearances = CardAppearances(own = mapOf(projects to ownLook), default = widgetDefault)

        assertEquals(ownLook, appearances.of(projects))
    }

    @Test
    fun `only a card with saved settings counts as set up on its own`() {
        val appearances = CardAppearances(own = mapOf(projects to ownLook), default = widgetDefault)

        assertTrue(appearances.hasOwn(projects))
        assertFalse(appearances.hasOwn(groceries))
    }
}
