package com.marfolog.noteswidgetformarkdown.util

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RateReminderTest {

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
    private val store = InMemoryStore()

    @Test
    fun `a fresh install records both dates and is not an update`() {
        assertFalse(RateReminder.recordStart(store, installedAt))
        assertEquals(installedAt, store.values["rating_first_seen_at"])
        assertEquals(installedAt, store.values["rate_chip_feature_seen_at"])
    }

    @Test
    fun `a Play user updating keeps their first start and gets the feature date now`() {
        val updatedAt = installedAt + 90 * day
        store.putLong("rating_first_seen_at", installedAt)

        assertTrue(RateReminder.recordStart(store, updatedAt))
        assertEquals(installedAt, store.values["rating_first_seen_at"])
        assertEquals(updatedAt, store.values["rate_chip_feature_seen_at"])
    }

    @Test
    fun `later starts change nothing and are not an update`() {
        RateReminder.recordStart(store, installedAt)

        assertFalse(RateReminder.recordStart(store, installedAt + 3 * day))
        assertEquals(installedAt, store.values["rating_first_seen_at"])
        assertEquals(installedAt, store.values["rate_chip_feature_seen_at"])
    }

    @Test
    fun `the chip window follows the recorded dates`() {
        RateReminder.recordStart(store, installedAt)

        assertNull(RateReminder.openWindow(store, installedAt + 2 * day))
        assertEquals(0, RateReminder.openWindow(store, installedAt + 3 * day))
    }

    @Test
    fun `nothing recorded yet means no chip`() {
        assertNull(RateReminder.openWindow(store, installedAt + 30 * day))
    }

    @Test
    fun `rate, dont ask again and the settings switch each hide the chip`() {
        RateReminder.recordStart(store, installedAt)
        val inWindow = installedAt + 4 * day

        RateReminder.setEnabled(store, false)
        assertNull(RateReminder.openWindow(store, inWindow))
        RateReminder.setEnabled(store, true)
        assertEquals(0, RateReminder.openWindow(store, inWindow))

        RateReminder.markNeverAgain(store)
        assertNull(RateReminder.openWindow(store, inWindow))
        store.values.remove("rate_chip_never_again")

        RateReminder.markRateTapped(store)
        assertNull(RateReminder.openWindow(store, inWindow))
    }

    @Test
    fun `each window is reported once, however often the widgets redraw`() {
        assertTrue(RateReminder.claimShownWindow(store, 0))
        assertFalse(RateReminder.claimShownWindow(store, 0))
        assertFalse(RateReminder.claimShownWindow(store, 0))
        assertTrue(RateReminder.claimShownWindow(store, 1))
        assertFalse(RateReminder.claimShownWindow(store, 1))
        assertTrue(RateReminder.claimShownWindow(store, 2))
        assertFalse(RateReminder.claimShownWindow(store, 1))
    }

    @Test
    fun `no open window is never reported and claims nothing`() {
        assertFalse(RateReminder.claimShownWindow(store, null))
        assertTrue(RateReminder.claimShownWindow(store, 0))
    }

    @Test
    fun `setting is hidden on a fresh install and appears three days later, whatever the chip state`() {
        RateReminder.recordStart(store, installedAt)
        assertFalse(RateReminder.isSettingAvailable(store, installedAt + 3 * day - 1))
        assertTrue(RateReminder.isSettingAvailable(store, installedAt + 3 * day))

        RateReminder.markRateTapped(store)
        RateReminder.markNeverAgain(store)
        RateReminder.setEnabled(store, false)
        assertTrue(RateReminder.isSettingAvailable(store, installedAt + 200 * day))
    }

    @Test
    fun `setting is hidden when nothing has been recorded yet`() {
        assertFalse(RateReminder.isSettingAvailable(store, installedAt + 200 * day))
    }
}
