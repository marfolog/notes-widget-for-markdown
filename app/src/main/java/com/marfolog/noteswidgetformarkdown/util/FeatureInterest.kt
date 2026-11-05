package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.prompt.PROMPTS
import com.marfolog.noteswidgetformarkdown.util.prompt.PromptIds
import com.marfolog.noteswidgetformarkdown.util.prompt.PromptSurfaces

/**
 * A question to see whether a feature that does not exist yet would be used: "would you use
 * widgets that each show a different folder?". Nothing is promised and nothing is built because
 * of one answer; it only counts. Asked in settings in a banner, and again as a small line under
 * the list of subfolders. An answer anywhere, or closing the banner, ends the question for good.
 */
object FeatureInterest {

    const val MIN_VISITS = 10

    /** A banner nobody reacts to is shown on this many visits and then never again. */
    const val MAX_BANNER_SHOWS = 3

    /** Whoever has notes in this many subfolders is the one for whom separate widgets make sense. */
    const val MIN_SUBFOLDERS_FOR_LIST_ROW = 2

    private const val KEY_ANSWERED_PREFIX = "feature_interest_answered_"
    private const val KEY_BANNER_SHOWS_PREFIX = "feature_interest_banner_shows_"
    private const val KEY_SENT_PREFIX = "feature_interest_sent_"
    private const val KEY_IGNORED_LOGGED_PREFIX = "feature_interest_ignored_logged_"

    /**
     * Whether the banner at the top of settings has earned its place: the guide is done, settings
     * were opened [MIN_VISITS] times since this build came onto the phone, and it has not been shown
     * [MAX_BANNER_SHOWS] times already without anyone reacting. Where and when it may appear next to
     * other prompts is not decided here but by the prompt coordinator.
     */
    fun isDue(answered: Boolean, guideDone: Boolean, visits: Int, bannerShows: Int = 0): Boolean =
        !answered && guideDone && visits >= MIN_VISITS && bannerShows < MAX_BANNER_SHOWS

    /**
     * The banner was shown as often as it will be and nobody answered or closed it: that is an answer
     * of its own, counted once, so that a question nobody reacted to can be told from one nobody saw.
     */
    internal fun isIgnored(answered: Boolean, bannerShows: Int, alreadyReported: Boolean): Boolean =
        !answered && !alreadyReported && bannerShows >= MAX_BANNER_SHOWS

    /** Whether this build asks at all: only the one that can count the answer lists the prompt. */
    val isAskedHere: Boolean get() = PROMPTS.any { it.id == PromptIds.INTEREST_BANNER }

    internal fun shouldShowListRow(answered: Boolean, subfoldersWithNotes: Int, asked: Boolean = isAskedHere): Boolean =
        asked && !answered && subfoldersWithNotes >= MIN_SUBFOLDERS_FOR_LIST_ROW

    fun isAnswered(context: Context, feature: String): Boolean = isAnswered(store(context), feature)

    internal fun isAnswered(store: KeyValueStore, feature: String): Boolean =
        store.getBoolean(KEY_ANSWERED_PREFIX + feature, false)

    /** Records the answer for good and counts it; [from] is where it was given. */
    fun answer(context: Context, feature: String, answer: String, from: String) {
        markAnswered(store(context), feature)
        PromptSurfaces.answered(context, PromptIds.INTEREST_BANNER)
        sendOnce(store(context), feature, answer)
        Telemetry.event(UsageEvent.FEATURE_INTEREST, *UsageEvent.featureInterest(feature, answer, from))
    }

    /**
     * A tap on "yes" or "no" is the user's own, so it is sent whether or not they agreed to
     * statistics, like the feedback form: once per installation and feature. Closing the banner is
     * not an answer and sends nothing.
     */
    internal fun sendOnce(
        store: KeyValueStore,
        feature: String,
        answer: String,
        asked: Boolean = isAskedHere,
        send: (String, String) -> Unit = InterestSender::send
    ): Boolean {
        if (!asked || !shouldSend(feature, answer, store.getBoolean(KEY_SENT_PREFIX + feature, false))) return false
        // Marked before the attempt: one try is all it gets, so a slow network never makes two documents.
        store.putBoolean(KEY_SENT_PREFIX + feature, true)
        send(feature, answer)
        return true
    }

    internal fun shouldSend(feature: String, answer: String, alreadySent: Boolean): Boolean =
        !alreadySent && InterestPayload.isSendable(feature, answer)

    internal fun markAnswered(store: KeyValueStore, feature: String) =
        store.putBoolean(KEY_ANSWERED_PREFIX + feature, true)

    fun bannerShows(context: Context, feature: String): Int =
        (store(context).getInt(KEY_BANNER_SHOWS_PREFIX + feature) ?: 0).coerceAtLeast(0)

    internal fun recordBannerShown(store: KeyValueStore, feature: String) =
        store.putInt(KEY_BANNER_SHOWS_PREFIX + feature, (store.getInt(KEY_BANNER_SHOWS_PREFIX + feature) ?: 0) + 1)

    /** Counted when the banner is put on screen, once per visit. */
    fun recordBannerShown(context: Context, feature: String) = recordBannerShown(store(context), feature)

    /** After the last showing, the first visit that finds the banner still unanswered reports it once. */
    fun reportIgnoredOnce(context: Context, feature: String, from: String) {
        val store = store(context)
        val reported = store.getBoolean(KEY_IGNORED_LOGGED_PREFIX + feature, false)
        if (!isIgnored(isAnswered(store, feature), bannerShows(context, feature), reported)) return
        store.putBoolean(KEY_IGNORED_LOGGED_PREFIX + feature, true)
        Telemetry.event(
            UsageEvent.FEATURE_INTEREST,
            *UsageEvent.featureInterest(feature, UsageEvent.ANSWER_IGNORED, from)
        )
    }

    fun reportShown(feature: String, from: String) {
        Telemetry.event(UsageEvent.FEATURE_INTEREST_SHOWN, *UsageEvent.featureInterestShown(feature, from))
    }

    private fun store(context: Context): KeyValueStore = SharedPreferencesStore(
        context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
    )
}
