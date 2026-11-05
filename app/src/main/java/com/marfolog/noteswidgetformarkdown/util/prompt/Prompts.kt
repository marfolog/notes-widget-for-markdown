package com.marfolog.noteswidgetformarkdown.util.prompt

import com.marfolog.noteswidgetformarkdown.util.ConsentPrompt
import com.marfolog.noteswidgetformarkdown.util.FeatureInterest
import com.marfolog.noteswidgetformarkdown.util.RatingAsk

/**
 * Every prompt the app can show, in one list. Their rules for when they are due stay in their own
 * objects ([ConsentPrompt], [com.marfolog.noteswidgetformarkdown.util.WhatsNew],
 * [com.marfolog.noteswidgetformarkdown.util.AfterNoteRating], [FeatureInterest]…); here they are
 * only given a kind, a place and a priority. The first-launch guide and the feedback form are not
 * prompts: somebody asked for them.
 *
 * ## Adding a prompt
 * 1. Put the rule for "is it due" in its own object, as a pure function with tests.
 * 2. If it needs something the surfaces do not know yet, add a field to [PromptFacts] and fill it
 *    where that surface builds its facts.
 * 3. Add one [PromptDef] to [CORE_PROMPTS] (or, for a prompt only one build has, to the `FlavorPrompts` twin of that build): an id, a [PromptKind], the [Surface]s, a priority (lower first), and
 *    the lists of what it keeps away from ([PromptDef.suppressedBy], [PromptDef.noSameDayAs]).
 * 4. Where it is shown, ask [PromptCoordinator] and show it only when its id comes back.
 * The coordinator needs no change. `PromptTimelineTest` shows the pattern with a made-up prompt.
 */
object PromptIds {
    const val CONSENT = "consent"
    const val WHATS_NEW = "whats_new"
    const val RATING_NATIVE = "rating_native"
    const val RATING_DIALOG = "rating_dialog"
    const val INTEREST_BANNER = "interest_banner"
    const val RATE_CHIP = "rate_chip"
    const val SYNC_CARD = "sync_card"
    const val FOLDER_CARD = "folder_card"
}

val CORE_PROMPTS: List<PromptDef> = listOf(
    PromptDef(
        id = PromptIds.CONSENT,
        kind = PromptKind.Interrupting,
        surfaces = setOf(Surface.Settings, Surface.AfterNote),
        priority = 10,
        isDue = { c ->
            val f = c.facts
            if (c.surface == Surface.AfterNote) {
                ConsentPrompt.isDueAfterNote(f.canReport, f.reportingAsked)
            } else {
                ConsentPrompt.isDue(f.canReport, f.reportingAsked, f.notesFolderChosen, f.widgetsPlaced, f.guideSkipped)
            }
        }
    ),
    PromptDef(
        id = PromptIds.WHATS_NEW,
        kind = PromptKind.Interrupting,
        surfaces = setOf(Surface.Settings),
        priority = 20,
        isDue = { it.facts.whatsNewDue },
        // A question that has to be answered is not held back by the quiet days.
        ignoresGap = { it.facts.whatsNewMandatory }
    ),
    PromptDef(
        id = PromptIds.RATING_NATIVE,
        kind = PromptKind.Interrupting,
        surfaces = setOf(Surface.AfterNote),
        priority = 30,
        isDue = { it.facts.ratingAsk == RatingAsk.Native },
        noSameDayAs = setOf(PromptIds.INTEREST_BANNER)
    ),
    PromptDef(
        id = PromptIds.RATING_DIALOG,
        kind = PromptKind.Interrupting,
        surfaces = setOf(Surface.AfterNote),
        priority = 31,
        isDue = { it.facts.ratingAsk == RatingAsk.Dialog },
        // The chip on the widget already asks for the same thing on its days.
        suppressedBy = setOf(PromptIds.RATE_CHIP),
        noSameDayAs = setOf(PromptIds.INTEREST_BANNER)
    ),
    PromptDef(
        id = PromptIds.RATE_CHIP,
        kind = PromptKind.Passive,
        surfaces = setOf(Surface.Widget),
        priority = 50,
        isDue = { it.facts.rateChipOpen }
    ),
    PromptDef(
        id = PromptIds.SYNC_CARD,
        kind = PromptKind.Informational,
        surfaces = setOf(Surface.Settings),
        priority = 60,
        isDue = { it.facts.syncCardVisible }
    ),
    PromptDef(
        id = PromptIds.FOLDER_CARD,
        kind = PromptKind.Informational,
        surfaces = setOf(Surface.Settings),
        priority = 61,
        isDue = { it.facts.folderCardVisible }
    )
)

/**
 * The question whether separate widgets for separate folders would be used. Its answer can only be
 * counted by the analytics, which only the Play build has, so only that build lists it: see
 * [FlavorPrompts].
 */
val INTEREST_BANNER_PROMPT = PromptDef(
        id = PromptIds.INTEREST_BANNER,
        kind = PromptKind.Banner,
        surfaces = setOf(Surface.Settings),
        priority = 40,
        isDue = { c ->
            c.facts.cardsKnown && FeatureInterest.isDue(
                c.facts.interestAnswered, c.facts.guideDone, c.facts.settingsVisits, c.facts.interestBannerShows
            )
        },
        gapAfterInterruptingMillis = PromptDef.BANNER_GAP_MILLIS,
        suppressedBy = setOf(PromptIds.SYNC_CARD, PromptIds.FOLDER_CARD),
        noSameDayAs = setOf(PromptIds.RATING_NATIVE, PromptIds.RATING_DIALOG)
    )

/** What the app can show: the prompts of both builds, and the ones only this build has. */
val PROMPTS: List<PromptDef> = CORE_PROMPTS + FlavorPrompts.prompts
