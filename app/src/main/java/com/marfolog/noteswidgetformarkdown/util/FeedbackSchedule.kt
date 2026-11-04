package com.marfolog.noteswidgetformarkdown.util

import java.util.concurrent.TimeUnit

/**
 * The limits on sending feedback. The form opens from the "Send feedback" row only; settings do not
 * offer it on their own.
 */
internal object FeedbackSchedule {

    val SEND_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(24)
    val PENDING_MILLIS = TimeUnit.SECONDS.toMillis(60)

    /**
     * At most one submission a day. A clock earlier than the last one does not block: the stored
     * time is then the one that cannot be trusted, and a wrong date must not lock feedback out.
     */
    fun canSend(nowMillis: Long, lastSentAtMillis: Long?): Boolean {
        if (lastSentAtMillis == null) return true
        val elapsed = nowMillis - lastSentAtMillis
        return elapsed < 0 || elapsed >= SEND_INTERVAL_MILLIS
    }

    /**
     * A send that timed out may still land a moment later, so for a minute another press of Send
     * would risk a second copy. A clock earlier than the start does not block, as in [canSend].
     */
    fun isPending(nowMillis: Long, pendingSinceMillis: Long?): Boolean {
        if (pendingSinceMillis == null) return false
        return nowMillis - pendingSinceMillis in 0 until PENDING_MILLIS
    }
}
