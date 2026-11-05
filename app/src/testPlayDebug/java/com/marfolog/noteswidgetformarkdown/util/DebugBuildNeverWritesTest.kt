package com.marfolog.noteswidgetformarkdown.util

import com.marfolog.noteswidgetformarkdown.BuildConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugBuildNeverWritesTest {
    @Test
    fun `the debug build is the one the guard stops`() {
        assertTrue(BuildConfig.DEBUG)
        assertFalse(FirestoreGuard.mayWrite(BuildConfig.DEBUG))
    }
}
