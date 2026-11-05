package com.marfolog.noteswidgetformarkdown.debug

import com.marfolog.noteswidgetformarkdown.BuildConfig
import com.marfolog.noteswidgetformarkdown.util.AppClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Time travel is for testing and must not be in the app people install: no offset in the clock, no
 * receiver that could move it, no text of the debug section. Runs only for the release builds.
 */
class ReleaseHasNoDebugToolsTest {

    @Test
    fun `this is a release build`() {
        assertFalse(BuildConfig.DEBUG)
    }

    @Test
    fun `the clock is the system clock and has nothing to move it with`() {
        val names = AppClock::class.java.declaredMethods.map { it.name }.toSet()
        assertEquals(setOf("init", "now"), names)
        assertTrue(AppClock.now() - System.currentTimeMillis() in -1_000..1_000)
    }

    @Test
    fun `the receiver that moves the clock does not exist`() {
        val missing = runCatching { Class.forName("com.marfolog.noteswidgetformarkdown.debug.DebugPromptsReceiver") }
        assertTrue(missing.exceptionOrNull() is ClassNotFoundException)
    }

    @Test
    fun `the debug tools are an empty twin with nothing to report or reset`() {
        val names = DebugTools::class.java.declaredMethods.map { it.name }.filterNot { it.contains("$") }.toSet()
        assertEquals(setOf("Section"), names)
    }
}
