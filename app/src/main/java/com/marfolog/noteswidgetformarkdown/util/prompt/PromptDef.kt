package com.marfolog.noteswidgetformarkdown.util.prompt

import com.marfolog.noteswidgetformarkdown.util.RatingAsk
import java.util.concurrent.TimeUnit

/**
 * How a prompt behaves towards the person using the app. [Interrupting] and [Banner] ask for
 * something and are what the coordinator rations; [Passive] things sit on screen without asking
 * (the "★ Rate" chip) and [Informational] ones report a state (the sync problem card). The last two
 * are never chosen, but a prompt can stay out of their way.
 */
enum class PromptKind {
    Interrupting, Banner, Passive, Informational;

    val isIntrusive: Boolean get() = this == Interrupting || this == Banner
}

/** Where a prompt may be shown. A surface asks the coordinator once per visit and shows what it gets. */
enum class Surface { Settings, AfterNote, Widget }

/** What the surfaces know and the prompts read. A prompt that needs a new fact adds a field here. */
data class PromptFacts(
    val canReport: Boolean = false,
    val reportingAsked: Boolean = true,
    val notesFolderChosen: Boolean = false,
    val widgetsPlaced: Int = 0,
    val guideSkipped: Boolean = false,
    val guideDone: Boolean = false,
    val setupComplete: Boolean = false,
    /** What settings were opened from, when it was one of the entry points that mean something. */
    val openedFrom: String? = null,
    val whatsNewDue: Boolean = false,
    val whatsNewMandatory: Boolean = false,
    val ratingAsk: RatingAsk = RatingAsk.None,
    val rateChipOpen: Boolean = false,
    val settingsVisits: Int = 0,
    val interestAnswered: Boolean = false,
    val interestBannerShows: Int = 0,
    val syncCardVisible: Boolean = false,
    val folderCardVisible: Boolean = false,
    /**
     * Whether the cards above have been loaded. They are read in the background, and a banner must
     * not take a screen that is about to get one, so it waits; a dialog does not need them.
     */
    val cardsKnown: Boolean = true
)

/** Everything a prompt may look at to say whether it is due. */
data class PromptContext(
    val nowMillis: Long,
    val surface: Surface,
    val facts: PromptFacts,
    val history: PromptHistory
)

/**
 * One prompt, declared. The coordinator knows nothing about any of them: adding a prompt is adding
 * a [PromptDef] to [PROMPTS], never a change to the rules.
 *
 * @property priority lower goes first when several are due.
 * @property isDue whether the prompt itself wants to be shown now. What it needs to remember between
 *   visits (answered, asked twice, "only once") it keeps itself; the coordinator only knows when it
 *   was shown.
 * @property ignoresGap a question that has to be answered, which the quiet days between dialogs
 *   do not hold back (the one-per-visit rule still does).
 * @property gapAfterInterruptingMillis how long to keep away from the last [PromptKind.Interrupting]
 *   one that was shown.
 * @property suppressedBy ids of [PromptKind.Passive] or [PromptKind.Informational] prompts that,
 *   while they are due (on screen), keep this one away.
 * @property noSameDayAs ids of prompts this one is never shown on the same day as.
 */
class PromptDef(
    val id: String,
    val kind: PromptKind,
    val surfaces: Set<Surface>,
    val priority: Int,
    val isDue: (PromptContext) -> Boolean,
    val ignoresGap: (PromptContext) -> Boolean = { false },
    val gapAfterInterruptingMillis: Long = DEFAULT_GAP_MILLIS,
    val suppressedBy: Set<String> = emptySet(),
    val noSameDayAs: Set<String> = emptySet()
) {
    companion object {
        val DEFAULT_GAP_MILLIS: Long = TimeUnit.DAYS.toMillis(3)
        val BANNER_GAP_MILLIS: Long = TimeUnit.DAYS.toMillis(1)
    }
}
