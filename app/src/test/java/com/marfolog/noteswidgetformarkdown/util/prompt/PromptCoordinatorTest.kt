package com.marfolog.noteswidgetformarkdown.util.prompt

import com.marfolog.noteswidgetformarkdown.util.InMemoryKeyValueStore
import com.marfolog.noteswidgetformarkdown.util.RatingAsk
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The rules of the coordinator one by one, with the rules of the old dialog budget carried over.
 * Whole days of use are in [PromptTimelineTest].
 */
/** Both builds' prompts together, so the rules are tested the same whichever build runs the tests. */
internal val TEST_PROMPTS: List<PromptDef> = CORE_PROMPTS + INTEREST_BANNER_PROMPT

class PromptCoordinatorTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val openedAt = 1_760_000_000_000L
    private val store = InMemoryKeyValueStore()
    private val history = PromptHistory(store, ZoneOffset.UTC)

    private val consentDue = PromptFacts(canReport = true, reportingAsked = false, guideSkipped = true)
    private val newsDue = PromptFacts(setupComplete = true, whatsNewDue = true)
    private val everything = consentDue.copy(whatsNewDue = true)

    private fun decide(
        facts: PromptFacts,
        now: Long = openedAt,
        surface: Surface = Surface.Settings,
        visit: PromptCoordinator.Visit = PromptCoordinator.Visit(),
        registry: List<PromptDef> = TEST_PROMPTS
    ) = PromptCoordinator.decide(registry, PromptContext(now, surface, facts, history), visit)?.id

    private fun shown(id: String, at: Long) = PromptCoordinator.markShown(history, TEST_PROMPTS.first { it.id == id }, at)

    @Test
    fun `nothing due means nothing is shown`() {
        assertNull(decide(PromptFacts()))
    }

    @Test
    fun `the only prompt due is shown when none came before`() {
        assertEquals(PromptIds.CONSENT, decide(consentDue))
        assertEquals(PromptIds.WHATS_NEW, decide(newsDue))
    }

    @Test
    fun `the reporting question goes before the news`() {
        assertEquals(PromptIds.CONSENT, decide(everything))
    }

    @Test
    fun `a second intrusive prompt in the same visit is refused`() {
        val visit = PromptCoordinator.Visit(shown = listOf(TEST_PROMPTS.first { it.id == PromptIds.WHATS_NEW }))
        assertNull(decide(newsDue, visit = visit))
        assertNull(decide(everything, visit = visit))
    }

    @Test
    fun `three days of quiet after a dialog, from exactly three days on it is allowed`() {
        val lastAt = openedAt - 3 * day
        shown(PromptIds.WHATS_NEW, lastAt)
        assertNull(decide(newsDue, now = lastAt + day))
        assertNull(decide(newsDue, now = lastAt + 3 * day - 1))
        assertEquals(PromptIds.WHATS_NEW, decide(newsDue, now = lastAt + 3 * day))
    }

    @Test
    fun `the quiet days hold back even the reporting question`() {
        shown(PromptIds.WHATS_NEW, openedAt)
        assertNull(decide(everything, now = openedAt + day))
    }

    @Test
    fun `a clock set before the last dialog reads as not yet`() {
        shown(PromptIds.WHATS_NEW, openedAt)
        assertNull(decide(newsDue, now = openedAt - 10 * day))
    }

    @Test
    fun `a dialog months ago no longer holds anything back`() {
        shown(PromptIds.WHATS_NEW, openedAt)
        assertEquals(PromptIds.WHATS_NEW, decide(newsDue, now = openedAt + 90 * day))
    }

    @Test
    fun `a question that has to be answered is not held back by the quiet days`() {
        shown(PromptIds.CONSENT, openedAt - day)
        assertEquals(PromptIds.WHATS_NEW, decide(newsDue.copy(whatsNewMandatory = true)))
    }

    @Test
    fun `a required question still waits for the next visit when another prompt was shown in this one`() {
        val visit = PromptCoordinator.Visit(shown = listOf(TEST_PROMPTS.first { it.id == PromptIds.CONSENT }))
        assertNull(decide(newsDue.copy(whatsNewMandatory = true), visit = visit))
    }

    @Test
    fun `during the quiet days only the required question may come, the others keep waiting`() {
        shown(PromptIds.CONSENT, openedAt - day)
        assertEquals(PromptIds.WHATS_NEW, decide(everything.copy(whatsNewMandatory = true)))
        assertNull(decide(consentDue))
    }

    @Test
    fun `right after the reporting question the required news come on the next visit`() {
        val facts = everything.copy(whatsNewMandatory = true)
        assertEquals(PromptIds.CONSENT, decide(facts))
        shown(PromptIds.CONSENT, openedAt)
        assertEquals(PromptIds.WHATS_NEW, decide(facts.copy(reportingAsked = true), now = openedAt + 60_000))
    }

    @Test
    fun `a visit somebody came to read the sync answer in gets no prompt at all`() {
        assertNull(decide(everything, visit = PromptCoordinator.Visit(quiet = true)))
    }

    @Test
    fun `a prompt appears only on the surfaces it is declared for`() {
        assertNull(decide(newsDue, surface = Surface.AfterNote))
        assertEquals(PromptIds.RATING_NATIVE, decide(PromptFacts(ratingAsk = RatingAsk.Native), surface = Surface.AfterNote))
        assertNull(decide(PromptFacts(ratingAsk = RatingAsk.Native), surface = Surface.Settings))
    }

    @Test
    fun `passive and informational prompts are never chosen`() {
        val facts = PromptFacts(rateChipOpen = true, syncCardVisible = true, folderCardVisible = true)
        Surface.entries.forEach { assertNull(decide(facts, surface = it)) }
    }

    @Test
    fun `the banner keeps a day from the last dialog, not three`() {
        val facts = PromptFacts(guideDone = true, settingsVisits = 10)
        shown(PromptIds.WHATS_NEW, openedAt - day + 1)
        assertNull(decide(facts))
        assertEquals(PromptIds.INTEREST_BANNER, decide(facts, now = openedAt + 1))
    }

    @Test
    fun `a dialog goes before the banner when both are due`() {
        assertEquals(PromptIds.WHATS_NEW, decide(newsDue.copy(guideDone = true, settingsVisits = 10)))
    }

    @Test
    fun `the banner counts as an intrusive prompt of the visit`() {
        val visit = PromptCoordinator.Visit(shown = listOf(TEST_PROMPTS.first { it.id == PromptIds.INTEREST_BANNER }))
        assertNull(decide(newsDue, visit = visit))
    }

    @Test
    fun `a banner shown does not start the three quiet days of the dialogs`() {
        shown(PromptIds.INTEREST_BANNER, openedAt - 1)
        assertEquals(PromptIds.WHATS_NEW, decide(newsDue))
    }

    @Test
    fun `an informational card on screen keeps the banner away, a dialog is not affected`() {
        val facts = PromptFacts(guideDone = true, settingsVisits = 10)
        assertNull(decide(facts.copy(syncCardVisible = true)))
        assertNull(decide(facts.copy(folderCardVisible = true)))
        assertEquals(PromptIds.WHATS_NEW, decide(newsDue.copy(syncCardVisible = true)))
    }

    @Test
    fun `the banner waits until the cards are known, a dialog does not`() {
        val banner = PromptFacts(guideDone = true, settingsVisits = 10)
        assertNull(decide(banner.copy(cardsKnown = false)))
        assertEquals(PromptIds.INTEREST_BANNER, decide(banner.copy(cardsKnown = true)))
        assertEquals(PromptIds.WHATS_NEW, decide(newsDue.copy(cardsKnown = false)))
    }

    @Test
    fun `the rating dialog keeps away on the days the rate chip is on the widget`() {
        val facts = PromptFacts(ratingAsk = RatingAsk.Dialog)
        assertNull(decide(facts.copy(rateChipOpen = true), surface = Surface.AfterNote))
        assertEquals(PromptIds.RATING_DIALOG, decide(facts, surface = Surface.AfterNote))
    }

    @Test
    fun `google's review card is not held back by the chip`() {
        val facts = PromptFacts(ratingAsk = RatingAsk.Native, rateChipOpen = true)
        assertEquals(PromptIds.RATING_NATIVE, decide(facts, surface = Surface.AfterNote))
    }

    @Test
    fun `after a note the reporting question goes first and the rating waits`() {
        val facts = PromptFacts(canReport = true, reportingAsked = false, ratingAsk = RatingAsk.Dialog)
        assertEquals(PromptIds.CONSENT, decide(facts, surface = Surface.AfterNote))
    }

    @Test
    fun `a dialog after a note also keeps the quiet days from the last one`() {
        shown(PromptIds.WHATS_NEW, openedAt - day)
        assertNull(decide(PromptFacts(ratingAsk = RatingAsk.Native), surface = Surface.AfterNote))
        assertNull(decide(PromptFacts(canReport = true, reportingAsked = false), surface = Surface.AfterNote))
    }

    @Test
    fun `the time an older version stored for its last dialog still counts`() {
        store.putLong("last_auto_prompt_at", openedAt - day)
        assertNull(decide(newsDue))
        assertEquals(PromptIds.WHATS_NEW, decide(newsDue, now = openedAt + 2 * day))
    }

    @Test
    fun `the time the news were shown by an older version is found under the new id`() {
        store.putLong("whats_new_shown_at", openedAt)
        assertEquals(openedAt, history.shownAt(PromptIds.WHATS_NEW))
        assertNull(history.shownAt(PromptIds.CONSENT))
    }

    @Test
    fun `shown and answered are kept apart`() {
        shown(PromptIds.INTEREST_BANNER, openedAt)
        assertEquals(openedAt, history.shownAt(PromptIds.INTEREST_BANNER))
        assertNull(history.answeredAt(PromptIds.INTEREST_BANNER))
        PromptCoordinator.markAnswered(history, PromptIds.INTEREST_BANNER, openedAt + 5)
        assertEquals(openedAt + 5, history.answeredAt(PromptIds.INTEREST_BANNER))
    }

    @Test
    fun `the history keeps what was shown last of each kind`() {
        shown(PromptIds.WHATS_NEW, openedAt)
        shown(PromptIds.INTEREST_BANNER, openedAt + day)
        assertEquals(openedAt, history.lastShownOn(PromptKind.Interrupting))
        assertEquals(openedAt + day, history.lastShownOn(PromptKind.Banner))
        assertNull(history.lastShownOn(PromptKind.Passive))
        assertEquals(openedAt, history.lastInterruptingAt())
    }

    @Test
    fun `a made-up prompt added to the registry is held to the same rules without touching the coordinator`() {
        val fake = PromptDef(
            id = "fake_survey",
            kind = PromptKind.Banner,
            surfaces = setOf(Surface.Settings),
            priority = 5,
            isDue = { true },
            gapAfterInterruptingMillis = 0
        )
        val registry = TEST_PROMPTS + fake
        // It goes before everything, since it said so,
        assertEquals("fake_survey", decide(everything, registry = registry))
        // and once shown it takes the visit: nothing else may follow it.
        assertNull(decide(everything, registry = registry, visit = PromptCoordinator.Visit(shown = listOf(fake))))
        // Its own list of what it keeps away from is honoured too.
        val careful = PromptDef(
            id = "careful", kind = PromptKind.Banner, surfaces = setOf(Surface.Settings), priority = 1,
            isDue = { true }, suppressedBy = setOf(PromptIds.SYNC_CARD)
        )
        assertEquals("careful", decide(PromptFacts(), registry = TEST_PROMPTS + careful))
        assertNull(decide(PromptFacts(syncCardVisible = true), registry = TEST_PROMPTS + careful))
    }

    @Test
    fun `two prompts that must not share a day keep apart even when the quiet days would allow it`() {
        val a = PromptDef("a", PromptKind.Banner, setOf(Surface.Settings), 1, { true }, gapAfterInterruptingMillis = 0, noSameDayAs = setOf("b"))
        val b = PromptDef("b", PromptKind.Banner, setOf(Surface.AfterNote), 1, { true }, gapAfterInterruptingMillis = 0)
        PromptCoordinator.markShown(history, b, openedAt)
        assertNull(decide(PromptFacts(), registry = listOf(a, b), now = openedAt + 1))
        // The next calendar day it is free again.
        val midnight = (openedAt / day + 1) * day
        assertEquals("a", decide(PromptFacts(), registry = listOf(a, b), now = midnight))
    }

    @Test
    fun `the day is the calendar day, not twenty four hours`() {
        val lateEvening = (openedAt / day) * day + 23 * 3_600_000L
        assertEquals(true, history.dayOf(lateEvening) == history.dayOf(lateEvening - 22 * 3_600_000L))
        assertEquals(false, history.dayOf(lateEvening) == history.dayOf(lateEvening + 2 * 3_600_000L))
    }

    @Test
    fun `quiet visit is the sync chip and nothing else`() {
        assertEquals(true, PromptSurfaces.isQuietVisit(UsageEvent.FROM_SYNC_CHIP))
        assertEquals(false, PromptSurfaces.isQuietVisit(UsageEvent.FROM_WIDGET))
        assertEquals(false, PromptSurfaces.isQuietVisit(null))
    }
}
