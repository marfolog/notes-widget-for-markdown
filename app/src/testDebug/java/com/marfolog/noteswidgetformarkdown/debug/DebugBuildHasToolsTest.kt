package com.marfolog.noteswidgetformarkdown.debug

import com.marfolog.noteswidgetformarkdown.BuildConfig
import com.marfolog.noteswidgetformarkdown.util.AppClock
import org.junit.Assert.assertTrue
import org.junit.Test

/** The other side of [ReleaseHasNoDebugToolsTest]: the debug build has what the release build must not. */
class DebugBuildHasToolsTest {

    @Test
    fun `this is a debug build`() {
        assertTrue(BuildConfig.DEBUG)
    }

    @Test
    fun `the clock can be moved and the receiver exists`() {
        val names = AppClock::class.java.declaredMethods.map { it.name }
        assertTrue("addDays" in names)
        Class.forName("com.marfolog.noteswidgetformarkdown.debug.DebugPromptsReceiver")
    }

    @Test
    fun `without a move the clock is the system clock`() {
        assertTrue(AppClock.now() - System.currentTimeMillis() in -1_000..1_000)
    }
}
