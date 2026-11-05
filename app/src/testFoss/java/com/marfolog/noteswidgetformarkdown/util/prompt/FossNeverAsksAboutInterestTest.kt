package com.marfolog.noteswidgetformarkdown.util.prompt

import com.marfolog.noteswidgetformarkdown.util.FeatureInterest
import java.time.ZoneOffset
import com.marfolog.noteswidgetformarkdown.util.InMemoryKeyValueStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** The foss build has no way to send an answer, so it has no question: nowhere, however often settings are opened. */
class FossNeverAsksAboutInterestTest {

    @Test
    fun `the registry of the foss build has no interest banner`() {
        assertFalse(PROMPTS.any { it.id == PromptIds.INTEREST_BANNER })
        assertFalse(FeatureInterest.isAskedHere)
    }

    @Test
    fun `settings opened many times with everything done show nothing in the foss build`() {
        val history = PromptHistory(InMemoryKeyValueStore(), ZoneOffset.UTC)
        val facts = PromptFacts(guideDone = true, setupComplete = true, settingsVisits = 50)
        val chosen = PromptCoordinator.decide(PROMPTS, PromptContext(1_760_000_000_000L, Surface.Settings, facts, history))
        assertNull(chosen)
    }

    @Test
    fun `the line under the list of subfolders is not shown in the foss build`() {
        assertFalse(FeatureInterest.shouldShowListRow(answered = false, subfoldersWithNotes = 5))
    }
}
