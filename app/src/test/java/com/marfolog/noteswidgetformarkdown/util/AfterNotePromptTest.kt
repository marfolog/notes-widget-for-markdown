package com.marfolog.noteswidgetformarkdown.util

import com.marfolog.noteswidgetformarkdown.util.prompt.PromptIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AfterNotePromptTest {

    @Test
    fun `nothing chosen shows nothing`() {
        assertEquals(AfterNotePrompt.None, AfterNotePrompt.of(null))
    }

    @Test
    fun `each prompt the coordinator can choose after a note maps to what the activity shows`() {
        assertEquals(AfterNotePrompt.Consent, AfterNotePrompt.of(PromptIds.CONSENT))
        assertEquals(AfterNotePrompt.NativeReview, AfterNotePrompt.of(PromptIds.RATING_NATIVE))
        assertEquals(AfterNotePrompt.Rating, AfterNotePrompt.of(PromptIds.RATING_DIALOG))
    }

    @Test
    fun `a prompt that does not belong after a note shows nothing there`() {
        assertEquals(AfterNotePrompt.None, AfterNotePrompt.of(PromptIds.WHATS_NEW))
        assertEquals(AfterNotePrompt.None, AfterNotePrompt.of(PromptIds.INTEREST_BANNER))
        assertEquals(AfterNotePrompt.None, AfterNotePrompt.of("something_else"))
    }

    @Test
    fun `only the consent and the rating dialogs are drawn by the app, the review card is Google's`() {
        assertTrue(AfterNotePrompt.Consent.showsDialog)
        assertTrue(AfterNotePrompt.Rating.showsDialog)
        assertFalse(AfterNotePrompt.NativeReview.showsDialog)
        assertFalse(AfterNotePrompt.None.showsDialog)
    }

    @Test
    fun `with nothing to show the editor of a new note opens at once`() {
        assertFalse(AfterNotePrompt.None.showsDialog)
    }
}
