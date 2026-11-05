package com.marfolog.noteswidgetformarkdown.util.prompt

import com.marfolog.noteswidgetformarkdown.util.InMemoryKeyValueStore
import com.marfolog.noteswidgetformarkdown.util.RatingAsk
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Days of use, event by event, and what showed up: the collisions the prompt coordinator exists for,
 * one scenario each. Every row of "day, where, what happened -> what was shown" is an assertion.
 */
class PromptTimelineTest {

    /** A phone with a clock, the history the coordinator keeps and the facts of one person. */
    private class Phone(var facts: PromptFacts) {
        private val day = TimeUnit.DAYS.toMillis(1)
        private val start = LocalDate.of(2026, 11, 5).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val store = InMemoryKeyValueStore()
        val history = PromptHistory(store, ZoneOffset.UTC)

        fun at(dayNumber: Int, hour: Int = 10, minute: Int = 0): Long =
            start + dayNumber * day + hour * 3_600_000L + minute * 60_000L

        /** One visit to settings. [change] is what happened since the last one. */
        fun settings(
            dayNumber: Int,
            hour: Int = 10,
            openedFrom: String? = null,
            change: (PromptFacts) -> PromptFacts = { it }
        ): String? {
            facts = change(facts).let { it.copy(settingsVisits = it.settingsVisits + 1, openedFrom = openedFrom) }
            return ask(Surface.Settings, at(dayNumber, hour), quiet = PromptSurfaces.isQuietVisit(openedFrom))
        }

        /** A note saved from the fast note dialog or the + button. */
        fun afterNote(dayNumber: Int, hour: Int = 10, minute: Int = 0, change: (PromptFacts) -> PromptFacts = { it }): String? {
            facts = change(facts)
            return ask(Surface.AfterNote, at(dayNumber, hour, minute))
        }

        /** The tap on "yes", "no" or the close button. */
        fun answerBanner() {
            facts = facts.copy(interestAnswered = true)
        }

        private fun ask(surface: Surface, now: Long, quiet: Boolean = false): String? {
            val chosen = PromptCoordinator.decide(
                TEST_PROMPTS, PromptContext(now, surface, facts, history), PromptCoordinator.Visit(quiet = quiet)
            ) ?: return null
            PromptCoordinator.markShown(history, chosen, now)
            // What each prompt does to the facts when it is shown and dealt with.
            facts = when (chosen.id) {
                PromptIds.CONSENT -> facts.copy(reportingAsked = true)
                PromptIds.WHATS_NEW -> facts.copy(whatsNewDue = false, whatsNewMandatory = false)
                PromptIds.RATING_NATIVE, PromptIds.RATING_DIALOG -> facts.copy(ratingAsk = RatingAsk.None)
                PromptIds.INTEREST_BANNER -> facts.copy(interestBannerShows = facts.interestBannerShows + 1)
                else -> facts
            }
            return chosen.id
        }
    }

    private val newUser = PromptFacts(
        canReport = true, reportingAsked = true, notesFolderChosen = true, widgetsPlaced = 1,
        guideDone = true, setupComplete = true
    )
    private val updatedFrom14 = PromptFacts(
        canReport = true, reportingAsked = false, notesFolderChosen = true, widgetsPlaced = 1,
        guideDone = true, setupComplete = true, whatsNewDue = true, whatsNewMandatory = true
    )

    // region A: a new user, the guide is done, settings are opened now and then

    @Test
    fun `a new user sees nothing for nine visits, then a banner nobody reacts to on three visits and never again`() {
        val phone = Phone(newUser)
        val days = listOf(0, 0, 1, 2, 3, 5, 7, 8, 10, 12, 14, 17, 20, 25)
        val shown = days.map { phone.settings(it) }
        // visits 1 to 9 are quiet, the tenth to twelfth show the banner, the rest never again
        assertEquals(
            List(9) { null } + List(3) { PromptIds.INTEREST_BANNER } + List(2) { null },
            shown
        )
    }

    @Test
    fun `a banner that was answered on its first showing is never shown again`() {
        val phone = Phone(newUser.copy(settingsVisits = 9))
        assertEquals(PromptIds.INTEREST_BANNER, phone.settings(0))
        phone.answerBanner()
        assertEquals(List(3) { null }, listOf(1, 5, 30).map { phone.settings(it) })
    }

    @Test
    fun `a banner answered on its second showing is shown only twice`() {
        val phone = Phone(newUser.copy(settingsVisits = 9))
        assertEquals(PromptIds.INTEREST_BANNER, phone.settings(0))
        assertEquals(PromptIds.INTEREST_BANNER, phone.settings(1))
        phone.answerBanner()
        assertNull(phone.settings(2))
    }

    @Test
    fun `a new user opening settings on day zero, two and three sees nothing`() {
        val phone = Phone(newUser)
        assertNull(phone.settings(0))
        assertNull(phone.settings(2))
        assertNull(phone.settings(3))
    }

    @Test
    fun `a new user who saves a note on day three is asked for google's review once`() {
        val phone = Phone(newUser)
        assertNull(phone.afterNote(1) { it.copy(ratingAsk = RatingAsk.None) })
        assertEquals(PromptIds.RATING_NATIVE, phone.afterNote(3) { it.copy(ratingAsk = RatingAsk.Native) })
        // the next note the same day: the review was an attempt, nothing is due
        assertNull(phone.afterNote(3, hour = 12))
    }

    @Test
    fun `the banner waits a day after google's review and never shares its day`() {
        val phone = Phone(newUser.copy(settingsVisits = 9))
        assertEquals(PromptIds.RATING_NATIVE, phone.afterNote(4, hour = 9) { it.copy(ratingAsk = RatingAsk.Native) })
        // the tenth visit, but the same evening
        assertNull(phone.settings(4, hour = 22))
        // the next morning is under the day of quiet, the next evening is free
        assertNull(phone.settings(5, hour = 8))
        assertEquals(PromptIds.INTEREST_BANNER, phone.settings(5, hour = 10))
    }

    // endregion

    // region B: updating from 1.4.0

    @Test
    fun `someone updating from 1_4 is asked about reporting first, the news come on the next visit`() {
        val phone = Phone(updatedFrom14)
        assertEquals(PromptIds.CONSENT, phone.settings(0, hour = 9))
        // required news, so the quiet days do not hold them back, but one prompt per visit does
        assertEquals(PromptIds.WHATS_NEW, phone.settings(0, hour = 9))
        assertNull(phone.settings(0, hour = 9))
    }

    @Test
    fun `after the update nothing else comes in the days that follow, the banner counts its visits from here`() {
        val phone = Phone(updatedFrom14)
        phone.settings(0)
        phone.settings(0, hour = 11)
        val following = (1..7).map { phone.settings(it) }
        assertEquals(List(7) { null }, following)
        // the tenth visit since the update
        assertEquals(PromptIds.INTEREST_BANNER, phone.settings(8))
    }

    @Test
    fun `an update without a required question shows the news after the quiet days, not before`() {
        val phone = Phone(updatedFrom14.copy(whatsNewMandatory = false, reportingAsked = true))
        assertEquals(PromptIds.WHATS_NEW, phone.settings(0))
        assertNull(phone.afterNote(0, hour = 11) { it.copy(ratingAsk = RatingAsk.Native) })
    }

    // endregion

    // region C: the news and a saved note on the same day

    @Test
    fun `the news in the morning and a rating after a note five minutes later are not two dialogs in an hour`() {
        val phone = Phone(newUser.copy(whatsNewDue = true))
        assertEquals(PromptIds.WHATS_NEW, phone.settings(10, hour = 9))
        assertNull(phone.afterNote(10, hour = 9, minute = 5) { it.copy(ratingAsk = RatingAsk.Native) })
        assertNull(phone.afterNote(10, hour = 20))
        // the rating is still due and comes once the three quiet days are over
        assertNull(phone.afterNote(12))
        assertEquals(PromptIds.RATING_NATIVE, phone.afterNote(13, hour = 9))
    }

    @Test
    fun `the rating after a note and the news in settings are not on the same day either way round`() {
        val phone = Phone(newUser.copy(whatsNewDue = true))
        assertEquals(PromptIds.RATING_NATIVE, phone.afterNote(10, hour = 9) { it.copy(ratingAsk = RatingAsk.Native) })
        assertNull(phone.settings(10, hour = 9))
        assertNull(phone.settings(12))
        assertEquals(PromptIds.WHATS_NEW, phone.settings(13))
    }

    // endregion

    // region D: the reporting question

    @Test
    fun `reporting not answered stays the first thing to ask, in settings`() {
        val phone = Phone(newUser.copy(reportingAsked = false, whatsNewDue = true, whatsNewMandatory = false))
        assertEquals(PromptIds.CONSENT, phone.settings(0))
        assertNull(phone.settings(1))
        assertNull(phone.settings(2))
        assertEquals(PromptIds.WHATS_NEW, phone.settings(3))
    }

    @Test
    fun `consent in settings and a note saved in the same hour show one dialog`() {
        val phone = Phone(newUser.copy(reportingAsked = false))
        assertEquals(PromptIds.CONSENT, phone.settings(0, hour = 10))
        assertNull(phone.afterNote(0, hour = 10, minute = 40) { it.copy(ratingAsk = RatingAsk.Dialog) })
    }

    @Test
    fun `consent after a note and settings opened right after it show one dialog`() {
        val phone = Phone(newUser.copy(reportingAsked = false, whatsNewDue = true))
        assertEquals(PromptIds.CONSENT, phone.afterNote(0, hour = 10))
        assertNull(phone.settings(0, hour = 10))
    }

    @Test
    fun `without a way to report there is no question to ask`() {
        val phone = Phone(newUser.copy(canReport = false, reportingAsked = false))
        assertNull(phone.settings(0))
        assertNull(phone.afterNote(5))
    }

    // endregion

    // region E: banner against the cards that explain a problem

    @Test
    fun `the banner is not shown on a visit with the sync problem card, and comes when the card is gone`() {
        val phone = Phone(newUser.copy(settingsVisits = 9))
        assertNull(phone.settings(0) { it.copy(syncCardVisible = true) })
        assertEquals(PromptIds.INTEREST_BANNER, phone.settings(1) { it.copy(syncCardVisible = false) })
    }

    @Test
    fun `the banner is not shown while the folder explanation is on screen`() {
        val phone = Phone(newUser.copy(settingsVisits = 9))
        assertNull(phone.settings(0) { it.copy(folderCardVisible = true) })
        assertEquals(PromptIds.INTEREST_BANNER, phone.settings(1) { it.copy(folderCardVisible = false) })
    }

    @Test
    fun `a dialog is still shown over the sync card when settings were not opened from its chip`() {
        val phone = Phone(newUser.copy(whatsNewDue = true, syncCardVisible = true))
        assertEquals(PromptIds.WHATS_NEW, phone.settings(0))
    }

    @Test
    fun `settings opened from the sync chip show nothing at all, however many things are due`() {
        val phone = Phone(updatedFrom14.copy(settingsVisits = 20, syncCardVisible = true))
        assertNull(phone.settings(0, openedFrom = UsageEvent.FROM_SYNC_CHIP))
        assertEquals(PromptIds.CONSENT, phone.settings(0, hour = 12))
    }

    // endregion

    // region F: the rate chip and the rating dialog

    @Test
    fun `the app's rating dialog stays away on the days the chip is on the widget`() {
        val phone = Phone(newUser)
        assertNull(phone.afterNote(20) { it.copy(ratingAsk = RatingAsk.Dialog, rateChipOpen = true) })
        assertEquals(PromptIds.RATING_DIALOG, phone.afterNote(22) { it.copy(rateChipOpen = false) })
    }

    @Test
    fun `the chip itself never counts as a prompt of the visit`() {
        val phone = Phone(newUser.copy(rateChipOpen = true, whatsNewDue = true))
        assertEquals(PromptIds.WHATS_NEW, phone.settings(0))
    }

    // endregion

    // region G: clocks and the end of the questions

    @Test
    fun `a clock moved back does not bring anything forward`() {
        val phone = Phone(newUser.copy(whatsNewDue = true))
        assertEquals(PromptIds.WHATS_NEW, phone.settings(10))
        assertNull(phone.afterNote(2) { it.copy(ratingAsk = RatingAsk.Native) })
    }

    @Test
    fun `a question answered is not asked again however long it goes on`() {
        val phone = Phone(newUser.copy(settingsVisits = 9))
        assertEquals(PromptIds.INTEREST_BANNER, phone.settings(0))
        phone.answerBanner()
        val later = (1..60 step 7).map { phone.settings(it) }
        assertEquals(List(later.size) { null }, later)
    }

    @Test
    fun `the history records each prompt once under its own id, in one place`() {
        val phone = Phone(newUser.copy(whatsNewDue = true))
        phone.settings(0)
        assertEquals(phone.at(0), phone.history.shownAt(PromptIds.WHATS_NEW))
        assertEquals(phone.at(0), phone.history.lastInterruptingAt())
        assertNull(phone.history.shownAt(PromptIds.INTEREST_BANNER))
    }

    // endregion
}
