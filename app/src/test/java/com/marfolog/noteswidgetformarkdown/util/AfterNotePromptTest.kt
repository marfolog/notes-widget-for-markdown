package com.marfolog.noteswidgetformarkdown.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AfterNotePromptTest {

    @Test
    fun `nothing due shows nothing`() {
        assertEquals(AfterNotePrompt.None, AfterNotePrompt.choose(consentDue = false, ask = RatingAsk.None))
    }

    @Test
    fun `google's review card is launched when only it is due`() {
        assertEquals(AfterNotePrompt.NativeReview, AfterNotePrompt.choose(consentDue = false, ask = RatingAsk.Native))
    }

    @Test
    fun `the app's own rating dialog is shown when only it is due`() {
        assertEquals(AfterNotePrompt.Rating, AfterNotePrompt.choose(consentDue = false, ask = RatingAsk.Dialog))
    }

    @Test
    fun `the reporting question is shown when only it is due`() {
        assertEquals(AfterNotePrompt.Consent, AfterNotePrompt.choose(consentDue = true, ask = RatingAsk.None))
    }

    @Test
    fun `the reporting question goes first when a rating is due too, and the rating waits`() {
        assertEquals(AfterNotePrompt.Consent, AfterNotePrompt.choose(consentDue = true, ask = RatingAsk.Native))
        assertEquals(AfterNotePrompt.Consent, AfterNotePrompt.choose(consentDue = true, ask = RatingAsk.Dialog))
    }

    @Test
    fun `only the consent and the rating dialogs are drawn by the app, the review card is Google's`() {
        assertTrue(AfterNotePrompt.Consent.showsDialog)
        assertTrue(AfterNotePrompt.Rating.showsDialog)
        assertFalse(AfterNotePrompt.NativeReview.showsDialog)
        assertFalse(AfterNotePrompt.None.showsDialog)
    }

    @Test
    fun `everything shown after a note counts against the budget of automatic dialogs, nothing shown does not`() {
        assertTrue(AfterNotePrompt.Consent.countsAsAutomaticDialog)
        assertTrue(AfterNotePrompt.Rating.countsAsAutomaticDialog)
        assertTrue(AfterNotePrompt.NativeReview.countsAsAutomaticDialog)
        assertFalse(AfterNotePrompt.None.countsAsAutomaticDialog)
    }

    @Test
    fun `with nothing to show the editor of a new note opens at once`() {
        assertFalse(AfterNotePrompt.None.showsDialog)
    }
}
