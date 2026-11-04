package com.marfolog.noteswidgetformarkdown.presentation.onboarding

import com.marfolog.noteswidgetformarkdown.presentation.onboarding.OnboardingStep.Buttons
import com.marfolog.noteswidgetformarkdown.presentation.onboarding.OnboardingStep.Consent
import com.marfolog.noteswidgetformarkdown.presentation.onboarding.OnboardingStep.Folder
import com.marfolog.noteswidgetformarkdown.presentation.onboarding.OnboardingStep.Welcome
import com.marfolog.noteswidgetformarkdown.presentation.onboarding.OnboardingStep.Widget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTest {

    private val fresh = OnboardingProgress(canReport = true, consent = null, notesFolderChosen = false, buttons = null)
    private val freshFoss = fresh.copy(canReport = false)
    private val allDone = OnboardingProgress(canReport = true, consent = false, notesFolderChosen = true, buttons = true)

    @Test
    fun `the play guide asks the reporting question right after the welcome`() {
        assertEquals(listOf(Welcome, Consent, Folder, Buttons, Widget), Onboarding.steps(canReport = true))
    }

    @Test
    fun `the foss guide has no reporting step, there is nothing to report`() {
        assertEquals(listOf(Welcome, Folder, Buttons, Widget), Onboarding.steps(canReport = false))
    }

    @Test
    fun `the step counter counts the steps of this build`() {
        assertEquals(5, Onboarding.count(fresh))
        assertEquals(4, Onboarding.count(freshFoss))
        assertEquals(2, Onboarding.position(Consent, fresh))
        assertEquals(2, Onboarding.position(Folder, freshFoss))
        assertEquals(5, Onboarding.position(Widget, fresh))
    }

    @Test
    fun `a fresh install starts with the welcome`() {
        assertEquals(Welcome, Onboarding.stepOnOpen(null, fresh))
        assertEquals(Welcome, Onboarding.stepOnOpen(null, freshFoss))
    }

    @Test
    fun `someone who already has a notes folder never sees the guide`() {
        assertNull(Onboarding.stepOnOpen(null, fresh.copy(notesFolderChosen = true)))
        assertNull(Onboarding.stepOnOpen(null, freshFoss.copy(notesFolderChosen = true)))
    }

    @Test
    fun `an existing user who never answered the reporting question is not sent through the guide for it`() {
        assertNull(Onboarding.stepOnOpen(null, fresh.copy(notesFolderChosen = true, consent = null)))
    }

    @Test
    fun `someone who skipped the old guide without a folder gets the new one`() {
        assertEquals(Welcome, Onboarding.stepOnOpen(null, fresh))
    }

    @Test
    fun `a stored step comes back when everything before it is done`() {
        val progress = allDone.copy(buttons = null)

        assertEquals(Buttons, Onboarding.stepOnOpen(Buttons, progress))
        assertEquals(Folder, Onboarding.stepOnOpen(Folder, progress))
        assertEquals(Widget, Onboarding.stepOnOpen(Widget, allDone))
    }

    @Test
    fun `a process killed behind the folder picker comes back to the folder step`() {
        assertEquals(Folder, Onboarding.stepOnOpen(Folder, fresh.copy(consent = true)))
        assertEquals(Folder, Onboarding.stepOnOpen(Folder, fresh.copy(consent = true, notesFolderChosen = true)))
    }

    @Test
    fun `a stored step goes back to the first one whose answer is missing`() {
        assertEquals(Consent, Onboarding.stepOnOpen(Buttons, fresh))
        assertEquals(Folder, Onboarding.stepOnOpen(Buttons, fresh.copy(consent = true)))
        assertEquals(Buttons, Onboarding.stepOnOpen(Widget, allDone.copy(buttons = null)))
    }

    @Test
    fun `a stored consent step on foss restarts the guide`() {
        assertEquals(Welcome, Onboarding.stepOnOpen(Consent, freshFoss))
    }

    @Test
    fun `a rotation reads the same stored step`() {
        val progress = fresh.copy(consent = false)

        assertEquals(Onboarding.stepOnOpen(Folder, progress), Onboarding.stepOnOpen(Folder, progress))
    }

    @Test
    fun `get started leads to the reporting question on play and to the folder on foss`() {
        assertEquals(Consent, Onboarding.next(Welcome, fresh))
        assertEquals(Folder, Onboarding.next(Welcome, freshFoss))
    }

    @Test
    fun `the reporting step cannot be left without an answer`() {
        assertEquals(Consent, Onboarding.next(Consent, fresh))
    }

    @Test
    fun `one tap on either answer is enough to move on to the folder`() {
        listOf(true, false).forEach { agreed ->
            assertEquals(Folder, Onboarding.next(Consent, fresh.copy(consent = agreed)))
        }
    }

    @Test
    fun `no answer is preselected, so a fresh install shows none`() {
        assertNull(Onboarding.consentChoice(asked = false, enabled = true))
        assertNull(fresh.consent)
    }

    @Test
    fun `either answer to the reporting question lets the guide go on`() {
        assertEquals(Folder, Onboarding.next(Consent, fresh.copy(consent = true)))
        assertEquals(Folder, Onboarding.next(Consent, fresh.copy(consent = false)))
    }

    @Test
    fun `the folder step cannot be left forward without a folder`() {
        assertEquals(Folder, Onboarding.next(Folder, fresh.copy(consent = true)))
    }

    @Test
    fun `with a folder picked the buttons step comes next`() {
        assertEquals(Buttons, Onboarding.next(Folder, fresh.copy(consent = true, notesFolderChosen = true)))
        assertEquals(Buttons, Onboarding.next(Folder, freshFoss.copy(notesFolderChosen = true)))
    }

    @Test
    fun `the buttons step cannot be left without a choice`() {
        assertEquals(Buttons, Onboarding.next(Buttons, allDone.copy(buttons = null)))
    }

    @Test
    fun `either choice about the buttons lets the guide go on to the widget`() {
        assertEquals(Widget, Onboarding.next(Buttons, allDone.copy(buttons = true)))
        assertEquals(Widget, Onboarding.next(Buttons, allDone.copy(buttons = false)))
    }

    @Test
    fun `the widget step is the last`() {
        assertNull(Onboarding.next(Widget, allDone))
    }

    @Test
    fun `back goes one step back and leaves the app from the welcome`() {
        assertEquals(Buttons, Onboarding.back(Widget, allDone))
        assertEquals(Folder, Onboarding.back(Buttons, allDone))
        assertEquals(Consent, Onboarding.back(Folder, allDone))
        assertEquals(Welcome, Onboarding.back(Consent, allDone))
        assertNull(Onboarding.back(Welcome, allDone))
    }

    @Test
    fun `back skips the reporting step on foss because it is not there`() {
        assertEquals(Welcome, Onboarding.back(Folder, freshFoss))
    }

    @Test
    fun `back from the folder step never lets anyone past it`() {
        val back = Onboarding.back(Folder, fresh.copy(consent = true))!!

        assertEquals(Folder, Onboarding.next(Folder, fresh.copy(consent = true)))
        assertEquals(Consent, back)
    }

    @Test
    fun `a pick is shown only once the question was asked`() {
        assertNull(Onboarding.consentChoice(asked = false, enabled = false))
        assertNull(Onboarding.consentChoice(asked = false, enabled = true))
        assertEquals(true, Onboarding.consentChoice(asked = true, enabled = true))
        assertEquals(false, Onboarding.consentChoice(asked = true, enabled = false))
    }

    @Test
    fun `every step but the welcome has a name in the analytics`() {
        assertEquals(
            listOf(null, "consent", "folder", "buttons", "widget"),
            OnboardingStep.entries.map { it.eventValue }
        )
    }

    @Test
    fun `the welcome is never reported, nothing may be sent before the question is answered`() {
        assertNull(Welcome.eventValue)
    }

    @Test
    fun `the widget step offers pinning on a launcher that supports it`() {
        assertEquals(WidgetStepState.Offer, Onboarding.widgetStepState(0, pinSupported = true, pinRequested = false))
    }

    @Test
    fun `a launcher that cannot pin gets the manual steps straight away`() {
        assertEquals(WidgetStepState.ManualHint, Onboarding.widgetStepState(0, pinSupported = false, pinRequested = false))
    }

    @Test
    fun `a pin request that came back without a widget shows the manual steps`() {
        assertEquals(WidgetStepState.ManualHint, Onboarding.widgetStepState(0, pinSupported = true, pinRequested = true))
    }

    @Test
    fun `a widget placed by any route ticks the step`() {
        assertEquals(WidgetStepState.Placed, Onboarding.widgetStepState(1, pinSupported = true, pinRequested = true))
        assertEquals(WidgetStepState.Placed, Onboarding.widgetStepState(2, pinSupported = false, pinRequested = false))
    }

    @Test
    fun `only an empty folder gets the warning, not one still being counted`() {
        assertTrue(Onboarding.warnsAboutEmptyFolder(0))
        assertFalse(Onboarding.warnsAboutEmptyFolder(null))
        assertFalse(Onboarding.warnsAboutEmptyFolder(3))
    }
}
