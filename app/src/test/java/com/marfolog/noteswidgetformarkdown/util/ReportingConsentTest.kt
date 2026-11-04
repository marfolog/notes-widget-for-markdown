package com.marfolog.noteswidgetformarkdown.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportingConsentTest {

    private class FakeSwitch : ReportingSwitch {
        var collecting = false
        var asked = false
        val calls = mutableListOf<String>()
        override fun setEnabled(enabled: Boolean) { collecting = enabled; calls += "enabled=$enabled" }
        override fun markAsked() { asked = true; calls += "asked" }
    }

    private val switch = FakeSwitch()

    @Test
    fun `sharing switches collection on and counts as answered`() {
        ReportingConsent.answer(switch, agreed = true)

        assertTrue(switch.collecting)
        assertTrue(switch.asked)
    }

    @Test
    fun `no thanks switches collection off, which is what discards what was not sent yet, and counts as answered`() {
        switch.collecting = true

        ReportingConsent.answer(switch, agreed = false)

        assertFalse(switch.collecting)
        assertTrue(switch.asked)
        assertEquals(listOf("enabled=false", "asked"), switch.calls)
    }

    @Test
    fun `collection is decided before the answer is recorded`() {
        ReportingConsent.answer(switch, agreed = true)

        assertEquals(listOf("enabled=true", "asked"), switch.calls)
    }

    @Test
    fun `whoever said no in the guide is not asked in settings`() {
        ReportingConsent.answer(switch, agreed = false)

        assertFalse(
            ConsentPrompt.isDue(
                canReport = true,
                alreadyAsked = switch.asked,
                notesFolderChosen = true,
                widgetsPlaced = 1,
                guideSkipped = true
            )
        )
    }

    @Test
    fun `whoever said yes in the guide is not asked in settings either`() {
        ReportingConsent.answer(switch, agreed = true)

        assertFalse(
            ConsentPrompt.isDue(
                canReport = true,
                alreadyAsked = switch.asked,
                notesFolderChosen = true,
                widgetsPlaced = 1,
                guideSkipped = false
            )
        )
    }

    @Test
    fun `an answer anywhere ends the question after a saved note too`() {
        assertTrue(ConsentPrompt.isDueAfterNote(canReport = true, alreadyAsked = false))

        ReportingConsent.answer(switch, agreed = false)

        assertFalse(ConsentPrompt.isDueAfterNote(canReport = true, alreadyAsked = switch.asked))
    }

    @Test
    fun `an existing user without an answer still gets the old dialog in settings`() {
        assertTrue(
            ConsentPrompt.isDue(
                canReport = true,
                alreadyAsked = false,
                notesFolderChosen = true,
                widgetsPlaced = 1,
                guideSkipped = false
            )
        )
    }

    @Test
    fun `the foss build is never asked after a saved note`() {
        assertFalse(ConsentPrompt.isDueAfterNote(canReport = false, alreadyAsked = false))
    }
}
