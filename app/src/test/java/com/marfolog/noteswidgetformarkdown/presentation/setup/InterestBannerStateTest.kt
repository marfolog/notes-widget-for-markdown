package com.marfolog.noteswidgetformarkdown.presentation.setup

import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InterestBannerStateTest {

    @Test
    fun `closing a banner that still asks counts as a dismissal`() {
        assertEquals(UsageEvent.ANSWER_DISMISSED, InterestBannerState.Ask.dismissAnswer())
    }

    @Test
    fun `closing the thanks after a yes counts as nothing, so nobody is both yes and dismissed`() {
        assertNull(InterestBannerState.Thanks.dismissAnswer())
    }

    @Test
    fun `a hidden banner has nothing to dismiss`() {
        assertNull(InterestBannerState.Hidden.dismissAnswer())
    }
}
