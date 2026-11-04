package com.marfolog.noteswidgetformarkdown.presentation.onboarding

import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.presentation.widget.AddButtons
import com.marfolog.noteswidgetformarkdown.util.InMemoryKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStoreTest {

    private val store = InMemoryKeyValueStore()

    @Test
    fun `nothing stored is no step and no notes folder`() {
        assertNull(OnboardingStore.savedStep(store))
        assertNull(OnboardingStore.notesUri(store))
    }

    @Test
    fun `someone who only ever picked the vault root has a notes folder, as the widget reads it`() {
        store.putString(SetupActivity.KEY_VAULT_URI, "content://vault")

        assertEquals("content://vault", OnboardingStore.notesUri(store))
    }

    @Test
    fun `the notes folder wins over the vault root`() {
        store.putString(SetupActivity.KEY_VAULT_URI, "content://vault")
        store.putString(SetupActivity.KEY_NOTES_URI, "content://notes")

        assertEquals("content://notes", OnboardingStore.notesUri(store))
    }

    @Test
    fun `an older user with only the vault root never sees the guide`() {
        store.putString(SetupActivity.KEY_VAULT_URI, "content://vault")
        val progress = OnboardingProgress(
            canReport = true,
            consent = null,
            notesFolderChosen = OnboardingStore.notesUri(store) != null,
            buttons = null
        )

        assertNull(Onboarding.stepOnOpen(null, progress))
    }

    @Test
    fun `a step survives the process being killed`() {
        OnboardingStore.save(store, OnboardingStep.Buttons)

        assertEquals(OnboardingStep.Buttons, OnboardingStore.savedStep(store))
    }

    @Test
    fun `an unknown stored step is treated as none`() {
        store.putString("onboarding_step", "tour")

        assertNull(OnboardingStore.savedStep(store))
    }

    @Test
    fun `finishing ends the guide and forgets its step but keeps the buttons setting`() {
        OnboardingStore.save(store, OnboardingStep.Widget)
        AddButtons.setShown(store, false)

        OnboardingStore.complete(store, widgetsPlaced = 1)

        assertNull(OnboardingStore.savedStep(store))
        assertEquals(true, store.values[SetupActivity.KEY_GUIDE_DONE])
        assertEquals(false, AddButtons.choice(store))
    }

    @Test
    fun `finishing with a widget is not a skip`() {
        OnboardingStore.complete(store, widgetsPlaced = 1)

        assertFalse(store.getBoolean(SetupActivity.KEY_GUIDE_SKIPPED, false))
    }

    @Test
    fun `finishing without a widget sets the flag the old guide set when it was skipped`() {
        OnboardingStore.complete(store, widgetsPlaced = 0)

        assertTrue(store.getBoolean(SetupActivity.KEY_GUIDE_SKIPPED, false))
        assertTrue(store.getBoolean(SetupActivity.KEY_GUIDE_DONE, false))
    }
}
