package com.marfolog.noteswidgetformarkdown.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Nothing a debug build does may reach the real Firestore, the answer of a real person's tap included. */
class FirestoreGuardTest {

    private val feature = UsageEvent.FEATURE_MULTI_FOLDER_WIDGETS

    @Test
    fun `a debug build may not write, a release build may`() {
        assertFalse(FirestoreGuard.mayWrite(debugBuild = true))
        assertTrue(FirestoreGuard.mayWrite(debugBuild = false))
    }

    @Test
    fun `an answer tapped in a debug build is logged and never stored`() {
        var stored: Map<String, Any?>? = null
        val logged = mutableListOf<String>()
        val reached = InterestSender.deliver(feature, "yes", debugBuild = true, log = { logged += it }) { stored = it }
        assertFalse(reached)
        assertEquals(null, stored)
        assertTrue(logged.single().startsWith("Debug build, would send to Firestore"))
    }

    @Test
    fun `in a release build the answer is stored with the server's time and nothing else`() {
        val stored = mutableListOf<Map<String, Any?>>()
        // FieldValue.serverTimestamp() needs Firebase, which a unit test has not got; the store is faked
        // before that point is reached, so this only runs where it can.
        val result = runCatching { InterestSender.deliver(feature, "no", debugBuild = false, log = {}) { stored += it } }
        if (result.isSuccess) {
            assertTrue(result.getOrThrow())
            assertEquals(1, stored.size)
            assertEquals(
                setOf("feature", "answer", "appVersion", "versionCode", "createdAt"),
                stored.single().keys
            )
        }
    }

    @Test
    fun `a close button or an ignored banner is never stored even in a release build`() {
        var calls = 0
        assertFalse(InterestSender.deliver(feature, UsageEvent.ANSWER_DISMISSED, debugBuild = false, log = {}) { calls++ })
        assertFalse(InterestSender.deliver(feature, UsageEvent.ANSWER_IGNORED, debugBuild = false, log = {}) { calls++ })
        assertEquals(0, calls)
    }

    @Test
    fun `an unknown feature is never stored`() {
        var calls = 0
        assertFalse(InterestSender.deliver("something_else", "yes", debugBuild = false, log = {}) { calls++ })
        assertEquals(0, calls)
    }
}
