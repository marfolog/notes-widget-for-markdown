package com.marfolog.noteswidgetformarkdown.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsentPromptTest {

    private fun due(
        canReport: Boolean = true,
        alreadyAsked: Boolean = false,
        notesFolderChosen: Boolean = true,
        widgetsPlaced: Int = 1,
        guideSkipped: Boolean = false
    ) = ConsentPrompt.isDue(canReport, alreadyAsked, notesFolderChosen, widgetsPlaced, guideSkipped)

    @Test
    fun `a fresh install is not asked before anything is set up`() {
        assertFalse(due(notesFolderChosen = false, widgetsPlaced = 0))
    }

    @Test
    fun `a notes folder without a widget on the home screen is not set up yet`() {
        assertFalse(due(notesFolderChosen = true, widgetsPlaced = 0))
    }

    @Test
    fun `a widget without a notes folder is not set up yet`() {
        assertFalse(due(notesFolderChosen = false, widgetsPlaced = 2))
    }

    @Test
    fun `once the folder is picked and a widget placed the question is due`() {
        assertTrue(due(notesFolderChosen = true, widgetsPlaced = 1))
        assertTrue(due(notesFolderChosen = true, widgetsPlaced = 3))
    }

    @Test
    fun `an answer either way means never again, even for someone who skipped the guide`() {
        assertFalse(due(alreadyAsked = true))
        assertFalse(due(alreadyAsked = true, guideSkipped = true, notesFolderChosen = false, widgetsPlaced = 0))
    }

    @Test
    fun `skipping the guide brings the question on a later visit without any setup`() {
        assertTrue(due(guideSkipped = true, notesFolderChosen = false, widgetsPlaced = 0))
    }

    @Test
    fun `a build that cannot report never asks`() {
        assertFalse(due(canReport = false))
        assertFalse(due(canReport = false, guideSkipped = true))
    }

    @Test
    fun `a nonsense widget count reads as no widget`() {
        assertFalse(due(widgetsPlaced = -1))
    }

    @Test
    fun `asked on the first visit after setup, then never again`() {
        var asked = false
        val visits = listOf(
            false to 0,
            true to 0,
            true to 1,
            true to 1
        )

        val shown = visits.map { (folder, widgets) ->
            due(alreadyAsked = asked, notesFolderChosen = folder, widgetsPlaced = widgets).also { if (it) asked = true }
        }

        assertEquals(listOf(false, false, true, false), shown)
    }
}
