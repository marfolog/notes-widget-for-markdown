package com.marfolog.noteswidgetformarkdown.util

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RateChipScheduleTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val installedAt = 1_760_000_000_000L

    private fun window(
        now: Long,
        firstSeen: Long? = installedAt,
        featureSeen: Long? = installedAt,
        whatsNewShown: Long? = null,
        rateTapped: Boolean = false,
        neverAgain: Boolean = false,
        disabled: Boolean = false
    ) = RateChipSchedule.openWindow(
        nowMillis = now,
        firstSeenAtMillis = firstSeen,
        featureSeenAtMillis = featureSeen,
        whatsNewShownAtMillis = whatsNewShown,
        rateTapped = rateTapped,
        neverAgain = neverAgain,
        disabledInSettings = disabled
    )

    @Test
    fun `new install sees nothing during its first three days`() {
        assertNull(window(now = installedAt))
        assertNull(window(now = installedAt + 3 * day - 1))
    }

    @Test
    fun `new install sees the chip exactly three days after the first start`() {
        assertEquals(0, window(now = installedAt + 3 * day))
    }

    @Test
    fun `first window lasts two days and closes on the dot`() {
        assertEquals(0, window(now = installedAt + 5 * day - 1))
        assertNull(window(now = installedAt + 5 * day))
    }

    @Test
    fun `nothing during the fourteen days between windows`() {
        assertNull(window(now = installedAt + 12 * day))
        assertNull(window(now = installedAt + 19 * day - 1))
    }

    @Test
    fun `second window opens sixteen days after the first`() {
        assertEquals(1, window(now = installedAt + 19 * day))
        assertEquals(1, window(now = installedAt + 21 * day - 1))
        assertNull(window(now = installedAt + 21 * day))
    }

    @Test
    fun `third window is the last one`() {
        assertEquals(2, window(now = installedAt + 35 * day))
        assertEquals(2, window(now = installedAt + 37 * day - 1))
        assertNull(window(now = installedAt + 37 * day))
    }

    @Test
    fun `no fourth window, not even years later`() {
        assertNull(window(now = installedAt + 51 * day))
        assertNull(window(now = installedAt + 52 * day))
        assertNull(window(now = installedAt + 3 * 365 * day))
    }

    @Test
    fun `someone using the app for months sees it two days after updating`() {
        val updatedAt = installedAt + 120 * day
        assertNull(window(now = updatedAt + 2 * day - 1, featureSeen = updatedAt))
        assertEquals(0, window(now = updatedAt + 2 * day, featureSeen = updatedAt))
        assertEquals(1, window(now = updatedAt + 18 * day, featureSeen = updatedAt))
    }

    @Test
    fun `someone who updated on day one already waited long enough for the two day rule`() {
        val updatedAt = installedAt + day
        assertEquals(installedAt + 3 * day, RateChipSchedule.firstShowAt(installedAt, updatedAt))
        assertEquals(0, window(now = installedAt + 3 * day, featureSeen = updatedAt))
    }

    @Test
    fun `someone who updated on day two waits the two days after the update, which are later than day three`() {
        val updatedAt = installedAt + 2 * day
        assertEquals(installedAt + 4 * day, RateChipSchedule.firstShowAt(installedAt, updatedAt))
        assertNull(window(now = installedAt + 4 * day - 1, featureSeen = updatedAt))
        assertEquals(0, window(now = installedAt + 4 * day, featureSeen = updatedAt))
    }

    @Test
    fun `tapping rate hides the chip for good`() {
        assertNull(window(now = installedAt + 3 * day, rateTapped = true))
        assertNull(window(now = installedAt + 19 * day, rateTapped = true))
    }

    @Test
    fun `dont ask again hides the chip for good`() {
        assertNull(window(now = installedAt + 3 * day, neverAgain = true))
        assertNull(window(now = installedAt + 35 * day, neverAgain = true))
    }

    @Test
    fun `switching the reminder off in settings hides it`() {
        assertNull(window(now = installedAt + 4 * day, disabled = true))
    }

    @Test
    fun `missing timestamps never show the chip`() {
        assertNull(window(now = installedAt + 4 * day, firstSeen = null))
        assertNull(window(now = installedAt + 4 * day, featureSeen = null))
        assertNull(window(now = installedAt + 4 * day, firstSeen = null, featureSeen = null))
    }

    @Test
    fun `a clock that went backwards shows nothing`() {
        assertNull(window(now = installedAt - day))
        val updatedAt = installedAt + 30 * day
        assertNull(window(now = updatedAt - 1, featureSeen = updatedAt))
    }

    @Test
    fun `shown is reported for the first redraw of each window only`() {
        assertTrue(RateChipSchedule.isFirstShowOfWindow(window = 0, lastReportedWindow = null))
        assertFalse(RateChipSchedule.isFirstShowOfWindow(window = 0, lastReportedWindow = 0))
        assertTrue(RateChipSchedule.isFirstShowOfWindow(window = 1, lastReportedWindow = 0))
        assertTrue(RateChipSchedule.isFirstShowOfWindow(window = 2, lastReportedWindow = 1))
        assertFalse(RateChipSchedule.isFirstShowOfWindow(window = 1, lastReportedWindow = 2))
    }

    @Test
    fun `nothing to report while no window is open`() {
        assertFalse(RateChipSchedule.isFirstShowOfWindow(window = null, lastReportedWindow = null))
        assertFalse(RateChipSchedule.isFirstShowOfWindow(window = null, lastReportedWindow = 0))
    }

    private fun available(now: Long, firstSeen: Long? = installedAt, featureSeen: Long? = installedAt) =
        RateChipSchedule.isSettingAvailable(now, firstSeen, featureSeen)

    @Test
    fun `setting stays hidden for a new install until day three`() {
        assertFalse(available(now = installedAt))
        assertFalse(available(now = installedAt + 3 * day - 1))
    }

    @Test
    fun `setting appears exactly when the first window opens`() {
        assertTrue(available(now = installedAt + 3 * day))
    }

    @Test
    fun `existing user gets the setting two days after the update`() {
        val updatedAt = installedAt + 90 * day
        assertFalse(available(now = updatedAt + 2 * day - 1, featureSeen = updatedAt))
        assertTrue(available(now = updatedAt + 2 * day, featureSeen = updatedAt))
    }

    @Test
    fun `setting stays hidden when a timestamp is missing`() {
        val later = installedAt + 400 * day
        assertFalse(available(now = later, firstSeen = null))
        assertFalse(available(now = later, featureSeen = null))
        assertFalse(available(now = later, firstSeen = null, featureSeen = null))
    }

    @Test
    fun `setting stays hidden when the clock is set before the first start`() {
        assertFalse(available(now = installedAt - 30 * day))
    }

    @Test
    fun `setting stays visible after every window has closed`() {
        assertTrue(available(now = installedAt + 3 * day + 100 * day))
        assertTrue(available(now = installedAt + 1000 * day))
    }

    @Test
    fun `an updated user who saw what's new waits a full week after it for the chip`() {
        val updatedAt = installedAt + 120 * day
        val whatsNewAt = updatedAt + day
        assertNull(window(now = updatedAt + 2 * day, featureSeen = updatedAt, whatsNewShown = whatsNewAt))
        assertNull(window(now = whatsNewAt + 7 * day - 1, featureSeen = updatedAt, whatsNewShown = whatsNewAt))
        assertEquals(0, window(now = whatsNewAt + 7 * day, featureSeen = updatedAt, whatsNewShown = whatsNewAt))
    }

    @Test
    fun `the windows that follow count from the later start the dialog gave`() {
        val updatedAt = installedAt + 120 * day
        val whatsNewAt = updatedAt + day
        assertEquals(1, window(now = whatsNewAt + 23 * day, featureSeen = updatedAt, whatsNewShown = whatsNewAt))
        assertNull(window(now = whatsNewAt + 55 * day, featureSeen = updatedAt, whatsNewShown = whatsNewAt))
    }

    @Test
    fun `what's new long before the chip was due does not push it back`() {
        val whatsNewAt = installedAt - 30 * day
        assertEquals(0, window(now = installedAt + 3 * day, whatsNewShown = whatsNewAt))
    }

    @Test
    fun `rating from what's new hides the chip for good`() {
        val whatsNewAt = installedAt + 10 * day
        assertNull(window(now = whatsNewAt + 7 * day, whatsNewShown = whatsNewAt, rateTapped = true))
    }

    @Test
    fun `a clock set before what's new was shown shows nothing`() {
        val whatsNewAt = installedAt + 60 * day
        assertNull(window(now = whatsNewAt - day, whatsNewShown = whatsNewAt))
    }

    @Test
    fun `the setting waits for the same week after what's new`() {
        val whatsNewAt = installedAt + 10 * day
        assertFalse(RateChipSchedule.isSettingAvailable(whatsNewAt + 7 * day - 1, installedAt, installedAt, whatsNewAt))
        assertTrue(RateChipSchedule.isSettingAvailable(whatsNewAt + 7 * day, installedAt, installedAt, whatsNewAt))
    }
}
