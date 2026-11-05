package com.marfolog.noteswidgetformarkdown.util.prompt

import com.marfolog.noteswidgetformarkdown.util.FeatureInterest
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Play build counts the answer through the analytics, so it is the one that asks. */
class PlayAsksAboutInterestTest {

    @Test
    fun `the registry of the play build has the interest banner`() {
        assertTrue(PROMPTS.any { it.id == PromptIds.INTEREST_BANNER })
        assertTrue(FeatureInterest.isAskedHere)
    }
}
