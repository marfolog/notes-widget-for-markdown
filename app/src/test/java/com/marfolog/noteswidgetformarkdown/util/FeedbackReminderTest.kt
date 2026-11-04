package com.marfolog.noteswidgetformarkdown.util

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackReminderTest {

    private class InMemoryStore : KeyValueStore {
        val values = mutableMapOf<String, Any>()
        override fun contains(key: String) = key in values
        override fun getLong(key: String) = values[key] as Long?
        override fun putLong(key: String, value: Long) { values[key] = value }
        override fun getInt(key: String) = values[key] as Int?
        override fun putInt(key: String, value: Int) { values[key] = value }
        override fun getString(key: String) = values[key] as String?
        override fun putString(key: String, value: String) { values[key] = value }
        override fun getBoolean(key: String, default: Boolean) = values[key] as Boolean? ?: default
        override fun putBoolean(key: String, value: Boolean) { values[key] = value }
        override fun remove(key: String) { values.remove(key) }
    }

    private val day = TimeUnit.DAYS.toMillis(1)
    private val installedAt = 1_760_000_000_000L
    private val store = InMemoryStore().also { RateReminder.recordStart(it, installedAt) }

    @Test
    fun `sending starts the one a day limit`() {
        val sentAt = installedAt + 9 * day
        FeedbackReminder.markSent(store, sentAt)

        assertEquals(sentAt, store.values["feedback_last_sent_at"])
        assertFalse(FeedbackReminder.canSend(store, sentAt + day - 1))
        assertTrue(FeedbackReminder.canSend(store, sentAt + day))
    }

    @Test
    fun `nothing sent yet means sending is allowed`() {
        assertTrue(FeedbackReminder.canSend(store, installedAt))
    }

    @Test
    fun `a send still on its way holds back a second press of send for a minute`() {
        val pressedAt = installedAt + 9 * day
        FeedbackReminder.markPending(store, pressedAt)

        assertTrue(FeedbackReminder.isPending(store, pressedAt + 30_000))
        assertFalse(FeedbackReminder.isPending(store, pressedAt + 60_000))
    }

    @Test
    fun `a send that failed outright can be retried at once`() {
        val pressedAt = installedAt + 9 * day
        FeedbackReminder.markPending(store, pressedAt)
        FeedbackReminder.clearPending(store)

        assertFalse(FeedbackReminder.isPending(store, pressedAt + 1))
    }

    @Test
    fun `a send that arrived is no longer pending`() {
        val pressedAt = installedAt + 9 * day
        FeedbackReminder.markPending(store, pressedAt)
        FeedbackReminder.markSent(store, pressedAt + 2_000)

        assertFalse(FeedbackReminder.isPending(store, pressedAt + 3_000))
        assertFalse(store.contains("feedback_pending_since"))
    }
}
