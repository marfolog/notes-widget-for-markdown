package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import java.util.concurrent.TimeUnit

/** The ways out of the rating dialog that follows a saved note. */
enum class AfterNoteChoice(val eventValue: String) {
    Rate(UsageEvent.CHOICE_RATE),
    Later(UsageEvent.CHOICE_LATER),

    /** Back or a tap beside the dialog: counted as [Later], reported apart. */
    Dismissed(UsageEvent.CHOICE_DISMISSED)
}

/** What, if anything, is to be asked for after a saved note. */
enum class RatingAsk { None, Native, Dialog }

/**
 * When a saved note is followed by a request for a rating, and which one. Google's own in-app review
 * comes first, in the Play build, because its stars sit right in the app. The app's own dialog is the
 * fallback, days later and never right before Google's, which forbids asking for an opinion ahead of
 * its card:
 *
 *  1. the first saved note two days or more after the first start asks Google's review, once
 *  2. five days after that, the app's own dialog — in the build without Google's, the same first
 *     saved note
 *  3. "Later" brings the dialog back after ten days, once; the second "Later" ends it for good
 *
 * Someone whose 1.3.1 already asked Google's review has had that attempt: the dialog follows five
 * days after it. Rating anywhere, at any point, ends everything; so does the dialog's own "Rate", and so does
 * "Don't ask again" on the widget's chip. The review is one attempt, however it went. A "What's new"
 * dialog on screen a moment ago holds it back for a day. Only a saved note counts: a failed save
 * asks for nothing.
 */
object AfterNoteRating {

    val MIN_AGE_MILLIS = TimeUnit.DAYS.toMillis(2)
    val DIALOG_AFTER_NATIVE_MILLIS = TimeUnit.DAYS.toMillis(5)
    val LATER_WAIT_MILLIS = TimeUnit.DAYS.toMillis(10)
    const val MAX_LATERS = 2
    val WHATS_NEW_QUIET_MILLIS = TimeUnit.DAYS.toMillis(1)

    private const val KEY_LATERS = "after_note_rating_laters"
    private const val KEY_LATER_AT = "after_note_rating_later_at"
    private const val KEY_NATIVE_AT = "after_note_rating_native_at"
    private const val KEY_ANSWERED = "after_note_rating_answered"

    /** Where 1.3.1 noted that it had asked Google's review, which is the same attempt as now. */
    private const val KEY_LEGACY_REVIEW_AT = "rating_last_request_at"

    /**
     * A clock earlier than the first start, the review or the last "Later" makes the gap negative,
     * which reads as "not yet". [firstSeenAtMillis] is null until the first start was recorded.
     */
    internal fun decide(
        saved: Boolean,
        nowMillis: Long,
        firstSeenAtMillis: Long?,
        nativeAvailable: Boolean,
        nativeRequestedAtMillis: Long?,
        laters: Int,
        laterAtMillis: Long?,
        answered: Boolean,
        ratedElsewhere: Boolean,
        whatsNewShownAtMillis: Long?
    ): RatingAsk {
        if (!saved || answered || ratedElsewhere || firstSeenAtMillis == null) return RatingAsk.None
        if (nowMillis - firstSeenAtMillis < MIN_AGE_MILLIS) return RatingAsk.None
        if (whatsNewShownAtMillis != null && nowMillis - whatsNewShownAtMillis < WHATS_NEW_QUIET_MILLIS) {
            return RatingAsk.None
        }
        if (nativeAvailable && nativeRequestedAtMillis == null) return RatingAsk.Native
        if (laters >= MAX_LATERS) return RatingAsk.None
        val waited = when {
            laters > 0 -> laterAtMillis != null && nowMillis - laterAtMillis >= LATER_WAIT_MILLIS
            nativeRequestedAtMillis != null -> nowMillis - nativeRequestedAtMillis >= DIALOG_AFTER_NATIVE_MILLIS
            else -> true
        }
        return if (waited) RatingAsk.Dialog else RatingAsk.None
    }

    /** [saved] is false after a failed save, which never asks. */
    fun askFor(context: Context, saved: Boolean, nowMillis: Long): RatingAsk =
        askFor(store(context), saved, nowMillis, RatingPrompt.NATIVE_REVIEW_AVAILABLE)

    internal fun askFor(store: KeyValueStore, saved: Boolean, nowMillis: Long, nativeAvailable: Boolean): RatingAsk =
        decide(
            saved = saved,
            nowMillis = nowMillis,
            firstSeenAtMillis = RateReminder.firstSeenAt(store),
            nativeAvailable = nativeAvailable,
            nativeRequestedAtMillis = (store.getLong(KEY_NATIVE_AT) ?: store.getLong(KEY_LEGACY_REVIEW_AT))?.takeIf { it >= 0 },
            laters = store.getInt(KEY_LATERS) ?: 0,
            laterAtMillis = store.getLong(KEY_LATER_AT)?.takeIf { it >= 0 },
            answered = store.getBoolean(KEY_ANSWERED, false),
            ratedElsewhere = RateReminder.rateTapped(store) || RateReminder.neverAskedAgain(store),
            whatsNewShownAtMillis = WhatsNewReminder.shownAt(store)
        )

    /** Google gives no word on whether its card was shown, so launching it is what counts. */
    fun recordNativeRequested(context: Context, nowMillis: Long) = recordNativeRequested(store(context), nowMillis)

    internal fun recordNativeRequested(store: KeyValueStore, nowMillis: Long) = store.putLong(KEY_NATIVE_AT, nowMillis)

    fun recordChoice(context: Context, choice: AfterNoteChoice, nowMillis: Long) =
        recordChoice(store(context), choice, nowMillis)

    internal fun recordChoice(store: KeyValueStore, choice: AfterNoteChoice, nowMillis: Long) {
        when (choice) {
            AfterNoteChoice.Rate -> {
                RateReminder.markRateTapped(store)
                store.putBoolean(KEY_ANSWERED, true)
            }
            AfterNoteChoice.Later, AfterNoteChoice.Dismissed -> {
                store.putInt(KEY_LATERS, (store.getInt(KEY_LATERS) ?: 0) + 1)
                store.putLong(KEY_LATER_AT, nowMillis)
            }
        }
    }

    /** For an answer given somewhere else, such as an idea sent from the widget's rate chip. */
    fun markAnswered(context: Context) = markAnswered(store(context))

    internal fun markAnswered(store: KeyValueStore) = store.putBoolean(KEY_ANSWERED, true)

    private fun store(context: Context): KeyValueStore = SharedPreferencesStore(
        context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
    )
}
