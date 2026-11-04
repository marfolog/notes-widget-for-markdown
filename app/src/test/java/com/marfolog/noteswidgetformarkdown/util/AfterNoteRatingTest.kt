package com.marfolog.noteswidgetformarkdown.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class AfterNoteRatingTest {

    private val store = InMemoryKeyValueStore()
    private val installedAt = 1_700_000_000_000L

    private fun day(n: Long) = installedAt + TimeUnit.DAYS.toMillis(n)

    private fun started() = RateReminder.recordStart(store, installedAt)

    private fun askFor(at: Long, saved: Boolean = true, play: Boolean = true) =
        AfterNoteRating.askFor(store, saved, at, nativeAvailable = play)

    /** What the activities do when the answer is Native: the card is launched and that is counted. */
    private fun askAndLaunchNative(at: Long) {
        assertEquals(RatingAsk.Native, askFor(at))
        AfterNoteRating.recordNativeRequested(store, at)
    }

    @Test
    fun `nobody is asked on the first day, however many notes they save`() {
        started()

        assertEquals(RatingAsk.None, askFor(day(0)))
        assertEquals(RatingAsk.None, askFor(day(0) + TimeUnit.HOURS.toMillis(20)))
    }

    @Test
    fun `one day and twenty three hours is not two days yet`() {
        started()

        assertEquals(RatingAsk.None, askFor(day(2) - TimeUnit.HOURS.toMillis(1)))
    }

    @Test
    fun `the first saved note on day two asks for google's review, no count of notes needed`() {
        started()

        assertEquals(RatingAsk.Native, askFor(day(2)))
    }

    @Test
    fun `a failed save asks for nothing, on day two and later`() {
        started()

        assertEquals(RatingAsk.None, askFor(day(2), saved = false))
        assertEquals(RatingAsk.None, askFor(day(30), saved = false))
    }

    @Test
    fun `without a recorded first start it never asks`() {
        assertEquals(RatingAsk.None, askFor(day(20)))
    }

    @Test
    fun `days three to six are quiet after google's review, and the next one is not at the same visit`() {
        started()
        askAndLaunchNative(day(2))

        assertEquals(RatingAsk.None, askFor(day(2)))
        assertEquals(RatingAsk.None, askFor(day(3)))
        assertEquals(RatingAsk.None, askFor(day(7) - 1))
    }

    @Test
    fun `five days after google's review the app's own dialog is the fallback`() {
        started()
        askAndLaunchNative(day(2))

        assertEquals(RatingAsk.Dialog, askFor(day(7)))
    }

    @Test
    fun `the five days count from the review, not from the first start`() {
        started()
        askAndLaunchNative(day(4))

        assertEquals(RatingAsk.None, askFor(day(8)))
        assertEquals(RatingAsk.Dialog, askFor(day(9)))
    }

    @Test
    fun `the review is one attempt however it went, a failed one included, and the dialog follows in five days`() {
        started()
        assertEquals(RatingAsk.Native, askFor(day(2)))
        AfterNoteRating.recordNativeRequested(store, day(2))

        assertEquals(RatingAsk.None, askFor(day(3)))
        assertEquals(RatingAsk.None, askFor(day(7) - 1))
        assertEquals(RatingAsk.Dialog, askFor(day(7)))
    }

    @Test
    fun `a review 1_3_1 already asked for is the attempt, the dialog follows five days after it`() {
        started()
        store.putLong("rating_last_request_at", day(2))

        assertEquals(RatingAsk.None, askFor(day(3)))
        assertEquals(RatingAsk.None, askFor(day(7) - 1))
        assertEquals(RatingAsk.Dialog, askFor(day(7)))
    }

    @Test
    fun `a review asked for months ago in 1_3_1 means the dialog is due at the first save`() {
        started()
        store.putLong("rating_last_request_at", day(-100))

        assertEquals(RatingAsk.Dialog, askFor(day(2)))
    }

    @Test
    fun `the new record wins over the one from 1_3_1`() {
        started()
        store.putLong("rating_last_request_at", day(-100))
        AfterNoteRating.recordNativeRequested(store, day(2))

        assertEquals(RatingAsk.None, askFor(day(3)))
    }

    @Test
    fun `dont ask again on the widget chip stops the review and the dialog after a note`() {
        started()
        RateReminder.markNeverAgain(store)

        assertEquals(RatingAsk.None, askFor(day(2)))
        assertEquals(RatingAsk.None, askFor(day(3650)))
    }

    @Test
    fun `dont ask again on the chip also stops the dialog after the review was tried`() {
        started()
        askAndLaunchNative(day(2))
        RateReminder.markNeverAgain(store)

        assertEquals(RatingAsk.None, askFor(day(7)))
    }

    @Test
    fun `later brings the dialog back after ten days, not nine`() {
        started()
        askAndLaunchNative(day(2))
        AfterNoteRating.recordChoice(store, AfterNoteChoice.Later, day(7))

        assertEquals(RatingAsk.None, askFor(day(7) + TimeUnit.DAYS.toMillis(9)))
        assertEquals(RatingAsk.Dialog, askFor(day(17)))
    }

    @Test
    fun `the second later ends the dialog for good, and google's card is not asked again`() {
        started()
        askAndLaunchNative(day(2))
        AfterNoteRating.recordChoice(store, AfterNoteChoice.Later, day(7))
        AfterNoteRating.recordChoice(store, AfterNoteChoice.Later, day(17))

        assertEquals(RatingAsk.None, askFor(day(27)))
        assertEquals(RatingAsk.None, askFor(day(3650)))
    }

    @Test
    fun `closing the dialog with back or beside it counts as later`() {
        started()
        askAndLaunchNative(day(2))
        AfterNoteRating.recordChoice(store, AfterNoteChoice.Dismissed, day(7))

        assertEquals(RatingAsk.None, askFor(day(16)))
        assertEquals(RatingAsk.Dialog, askFor(day(17)))
        AfterNoteRating.recordChoice(store, AfterNoteChoice.Dismissed, day(17))
        assertEquals(RatingAsk.None, askFor(day(3650)))
    }

    @Test
    fun `after rate in the dialog nothing asks anywhere, google's card included`() {
        started()
        AfterNoteRating.recordChoice(store, AfterNoteChoice.Rate, day(2))

        assertEquals(RatingAsk.None, askFor(day(3)))
        assertEquals(RatingAsk.None, askFor(day(3650)))
    }

    @Test
    fun `rate in the dialog also counts as rated for the widget chip`() {
        started()
        AfterNoteRating.recordChoice(store, AfterNoteChoice.Rate, day(7))

        assertTrue(RateReminder.rateTapped(store))
    }

    @Test
    fun `rate tapped anywhere else, the chip, what's new or settings, means nothing asks`() {
        started()
        RateReminder.markRateTapped(store)

        assertEquals(RatingAsk.None, askFor(day(2)))
        assertEquals(RatingAsk.None, askFor(day(3650)))
    }

    @Test
    fun `rate tapped after google's review ends the dialog too`() {
        started()
        askAndLaunchNative(day(2))
        RateReminder.markRateTapped(store)

        assertEquals(RatingAsk.None, askFor(day(7)))
    }

    @Test
    fun `an idea sent from the widget chip means nothing asks`() {
        started()
        AfterNoteRating.markAnswered(store)

        assertEquals(RatingAsk.None, askFor(day(2)))
    }

    @Test
    fun `the widget chip switched off in settings does not stop the review`() {
        started()
        RateReminder.setEnabled(store, false)

        assertEquals(RatingAsk.Native, askFor(day(2)))
    }

    @Test
    fun `the build without google's review asks with its own dialog on day two`() {
        started()

        assertEquals(RatingAsk.None, askFor(day(1), play = false))
        assertEquals(RatingAsk.Dialog, askFor(day(2), play = false))
    }

    @Test
    fun `without google's review later waits ten days, and the second later ends it`() {
        started()
        AfterNoteRating.recordChoice(store, AfterNoteChoice.Later, day(2))

        assertEquals(RatingAsk.None, askFor(day(11), play = false))
        assertEquals(RatingAsk.Dialog, askFor(day(12), play = false))

        AfterNoteRating.recordChoice(store, AfterNoteChoice.Later, day(12))
        assertEquals(RatingAsk.None, askFor(day(3650), play = false))
    }

    @Test
    fun `the build without google's review never asks for a card`() {
        started()
        AfterNoteRating.recordChoice(store, AfterNoteChoice.Later, day(2))
        AfterNoteRating.recordChoice(store, AfterNoteChoice.Later, day(12))

        assertEquals(RatingAsk.None, askFor(day(100), play = false))
    }

    @Test
    fun `a rate in the build without google's review ends everything`() {
        started()
        AfterNoteRating.recordChoice(store, AfterNoteChoice.Rate, day(2))

        assertEquals(RatingAsk.None, askFor(day(3650), play = false))
    }

    @Test
    fun `what's new a moment ago holds the review back for a day`() {
        started()
        WhatsNewReminder.markShown(store, "1.4.0", day(4))

        assertEquals(RatingAsk.None, askFor(day(4) + TimeUnit.HOURS.toMillis(23)))
        assertEquals(RatingAsk.Native, askFor(day(5)))
    }

    @Test
    fun `what's new holds the dialog back for a day too`() {
        started()
        askAndLaunchNative(day(2))
        WhatsNewReminder.markShown(store, "1.4.0", day(7))

        assertEquals(RatingAsk.None, askFor(day(7) + TimeUnit.HOURS.toMillis(23)))
        assertEquals(RatingAsk.Dialog, askFor(day(8)))
    }

    @Test
    fun `a clock that went backwards reads as not yet`() {
        started()
        askAndLaunchNative(day(10))

        assertEquals(RatingAsk.None, askFor(day(5)))
        assertEquals(RatingAsk.None, askFor(day(14)))
    }

    @Test
    fun `a clock set before the last later reads as not yet`() {
        started()
        askAndLaunchNative(day(2))
        AfterNoteRating.recordChoice(store, AfterNoteChoice.Later, day(40))

        assertEquals(RatingAsk.None, askFor(day(20)))
    }

    @Test
    fun `the whole timeline of the play build, day by day`() {
        started()
        assertEquals(RatingAsk.None, askFor(day(1)))

        askAndLaunchNative(day(2))
        listOf(3L, 4L, 5L, 6L).forEach { assertEquals(RatingAsk.None, askFor(day(it))) }

        assertEquals(RatingAsk.Dialog, askFor(day(7)))
        AfterNoteRating.recordChoice(store, AfterNoteChoice.Later, day(7))
        assertEquals(RatingAsk.None, askFor(day(16)))

        assertEquals(RatingAsk.Dialog, askFor(day(17)))
        AfterNoteRating.recordChoice(store, AfterNoteChoice.Later, day(17))
        assertEquals(RatingAsk.None, askFor(day(27)))
    }

    @Test
    fun `choices are reported with the shared rate dialog values and without an idea`() {
        assertEquals(listOf("rate", "later", "dismissed"), AfterNoteChoice.entries.map { it.eventValue })
    }
}
