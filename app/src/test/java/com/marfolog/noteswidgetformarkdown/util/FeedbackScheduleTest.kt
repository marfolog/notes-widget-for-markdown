package com.marfolog.noteswidgetformarkdown.util

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackScheduleTest {

    private val hour = TimeUnit.HOURS.toMillis(1)
    private val day = TimeUnit.DAYS.toMillis(1)
    private val installedAt = 1_760_000_000_000L
    private val aWeekLater = installedAt + 7 * day

    @Test
    fun `the first ever send is allowed`() {
        assertTrue(FeedbackSchedule.canSend(nowMillis = aWeekLater, lastSentAtMillis = null))
    }

    @Test
    fun `a second send within a day is refused, from exactly a day on it is allowed`() {
        val sentAt = aWeekLater
        assertFalse(FeedbackSchedule.canSend(nowMillis = sentAt, lastSentAtMillis = sentAt))
        assertFalse(FeedbackSchedule.canSend(nowMillis = sentAt + day - 1, lastSentAtMillis = sentAt))
        assertTrue(FeedbackSchedule.canSend(nowMillis = sentAt + day, lastSentAtMillis = sentAt))
    }

    @Test
    fun `a clock set before the last send does not lock feedback out`() {
        assertTrue(FeedbackSchedule.canSend(nowMillis = aWeekLater - 30 * day, lastSentAtMillis = aWeekLater))
    }

    @Test
    fun `nothing on its way means nothing is pending`() {
        assertFalse(FeedbackSchedule.isPending(nowMillis = aWeekLater, pendingSinceMillis = null))
    }

    @Test
    fun `a send that timed out blocks a retry for exactly a minute`() {
        val startedAt = aWeekLater
        val minute = TimeUnit.MINUTES.toMillis(1)
        assertTrue(FeedbackSchedule.isPending(nowMillis = startedAt, pendingSinceMillis = startedAt))
        assertTrue(FeedbackSchedule.isPending(nowMillis = startedAt + minute - 1, pendingSinceMillis = startedAt))
        assertFalse(FeedbackSchedule.isPending(nowMillis = startedAt + minute, pendingSinceMillis = startedAt))
    }

    @Test
    fun `a clock set before the timed out send does not block a retry`() {
        assertFalse(FeedbackSchedule.isPending(nowMillis = aWeekLater - hour, pendingSinceMillis = aWeekLater))
    }
}
