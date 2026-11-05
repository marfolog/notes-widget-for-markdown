package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.app.Activity
import com.marfolog.noteswidgetformarkdown.util.AfterNotePrompt
import com.marfolog.noteswidgetformarkdown.util.AfterNoteRating
import com.marfolog.noteswidgetformarkdown.util.AppClock
import com.marfolog.noteswidgetformarkdown.util.NativeReview
import com.marfolog.noteswidgetformarkdown.util.RateReminder
import com.marfolog.noteswidgetformarkdown.util.RatingPrompt
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import com.marfolog.noteswidgetformarkdown.util.prompt.PromptCoordinator
import com.marfolog.noteswidgetformarkdown.util.prompt.PromptFacts
import com.marfolog.noteswidgetformarkdown.util.prompt.PromptIds
import com.marfolog.noteswidgetformarkdown.util.prompt.PromptSurfaces
import com.marfolog.noteswidgetformarkdown.util.prompt.Surface
import kotlinx.coroutines.CoroutineScope

/** How long a save waits for Google's review card, if Play has not handed it over by then. */
private const val NATIVE_REVIEW_WAIT_MILLIS = 1_500L

/**
 * What an activity that saves a note asks for once it has. Built when the activity starts, so that
 * Google's review, when it is due, is requested ahead of the save and launching it holds nothing up;
 * [decide] then settles the one thing to show. [from] says which activity this is in the analytics.
 */
internal class AfterNoteAsk(
    private val activity: Activity,
    scope: CoroutineScope,
    private val from: String
) {
    private val context = activity.applicationContext
    private var visit = PromptCoordinator.Visit()
    private val review: NativeReview? =
        if (wantsNativeReview()) RatingPrompt.prepareNativeReview(activity, scope) else null

    /** What the prompts need to know after a saved note; the rules for what is due live with them. */
    private fun facts(): PromptFacts {
        val now = AppClock.now()
        return PromptFacts(
            canReport = Telemetry.ENABLED,
            reportingAsked = Telemetry.hasBeenAsked(context),
            ratingAsk = AfterNoteRating.askFor(context, saved = true, now),
            rateChipOpen = RateReminder.openWindow(context, now) != null
        )
    }

    private fun wantsNativeReview(): Boolean =
        PromptSurfaces.peek(context, Surface.AfterNote, facts())?.id == PromptIds.RATING_NATIVE

    /** [saved] is false after a failed save, which asks for nothing. */
    fun decide(saved: Boolean): AfterNotePrompt {
        if (!saved) return AfterNotePrompt.None
        val chosen = PromptSurfaces.decide(context, Surface.AfterNote, facts(), visit)
        if (chosen != null) visit = PromptCoordinator.Visit(visit.shown + chosen)
        val prompt = AfterNotePrompt.of(chosen?.id)
        if (prompt == AfterNotePrompt.Rating) {
            Telemetry.event(UsageEvent.RATE_DIALOG_SHOWN, UsageEvent.PARAM_FROM to from)
        }
        return prompt
    }

    /**
     * One attempt at Google's card, and it counts however it goes: an error, no Play Store or no
     * answer within the wait is an attempt made, and the app's own dialog follows in due time.
     */
    suspend fun launchNativeReview() {
        AfterNoteRating.recordNativeRequested(context, AppClock.now())
        Telemetry.event(UsageEvent.RATE_NATIVE_REQUESTED, UsageEvent.PARAM_FROM to from)
        review?.launch(activity, NATIVE_REVIEW_WAIT_MILLIS) { durationMillis ->
            Telemetry.event(
                UsageEvent.RATE_NATIVE_FINISHED,
                UsageEvent.PARAM_FROM to from,
                UsageEvent.PARAM_DURATION to UsageEvent.nativeDuration(durationMillis)
            )
        }
    }
}
