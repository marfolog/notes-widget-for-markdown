package com.marfolog.noteswidgetformarkdown.util

import com.marfolog.noteswidgetformarkdown.BuildConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseBuildMayWriteTest {
    @Test
    fun `the release build is the one the guard lets through`() {
        assertFalse(BuildConfig.DEBUG)
        assertTrue(FirestoreGuard.mayWrite(BuildConfig.DEBUG))
    }
}
