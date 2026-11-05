package com.marfolog.noteswidgetformarkdown.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncProblemReportTest {

    private val store = InMemoryKeyValueStore()

    @Test
    fun `a first trouble is reported`() {
        assertTrue(SyncProblemReport.claim(store, "cant_connect"))
    }

    @Test
    fun `the same trouble seen on every widget refresh is reported once`() {
        assertTrue(SyncProblemReport.claim(store, "cant_connect"))
        assertFalse(SyncProblemReport.claim(store, "cant_connect"))
        assertFalse(SyncProblemReport.claim(store, "cant_connect"))
    }

    @Test
    fun `moving from one trouble to another is a new transition`() {
        assertTrue(SyncProblemReport.claim(store, "stale"))
        assertTrue(SyncProblemReport.claim(store, "cant_connect"))
    }

    @Test
    fun `recovering and failing again is reported again`() {
        assertTrue(SyncProblemReport.claim(store, "not_pushed"))
        assertFalse(SyncProblemReport.claim(store, null))
        assertTrue(SyncProblemReport.claim(store, "not_pushed"))
    }

    @Test
    fun `a healthy chip reports nothing and remembers nothing`() {
        assertFalse(SyncProblemReport.claim(store, null))
        assertEquals(null, store.getString("sync_problem_reported_type"))
    }
}
