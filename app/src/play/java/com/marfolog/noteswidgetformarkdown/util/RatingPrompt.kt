package com.marfolog.noteswidgetformarkdown.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.annotation.StringRes
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.marfolog.noteswidgetformarkdown.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Play flavour: where a rating goes. Every way to rate the app itself — the widget chip, the dialog
 * after a saved note, "What's new" and settings — opens the store page; the app never sees what was
 * rated. Besides that, [NativeReview] is Google's own in-app review card. The foss flavour ships a
 * twin of this file that points at GitHub and has no such card.
 */
object RatingPrompt {

    const val NATIVE_REVIEW_AVAILABLE = true

    @StringRes
    val AFTER_NOTE_TITLE_RES = R.string.after_note_rating_title_play

    @StringRes
    val RATE_BODY_RES = R.string.rate_dialog_body_play

    @StringRes
    val RATE_BUTTON_RES = R.string.rate_dialog_rate_play

    @StringRes
    val WHATS_NEW_FOOTER_RES = R.string.whats_new_footer_play

    /** The row in the settings feedback section, where an icon already stands in for the star. */
    @StringRes
    val ABOUT_RATE_RES = R.string.about_rate

    private const val AREA = "Rating"

    fun openRatePage(context: Context) = openStorePage(context)

    /** The store page as an intent: the web address, which the Play Store app opens itself. */
    fun ratePageIntent(context: Context): Intent =
        ProjectLinks.viewIntent("https://play.google.com/store/apps/details?id=${context.packageName}")

    /**
     * Asks Play for the review ahead of time, when an activity starts, so launching it after a save
     * does not hold the save up. Nothing is shown until [NativeReview.launch].
     */
    fun prepareNativeReview(activity: Activity, scope: CoroutineScope): NativeReview {
        val manager = ReviewManagerFactory.create(activity)
        val info = scope.async {
            try {
                manager.requestReview()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.d(AREA, "Review not available: ${e.javaClass.simpleName}")
                null
            }
        }
        return NativeReview(manager, info)
    }

    /** The Play Store app if there is one, the web page otherwise. */
    fun openStorePage(context: Context) {
        val packageName = context.packageName
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        )
        try {
            context.startActivity(market)
        } catch (e: ActivityNotFoundException) {
            runCatching { context.startActivity(web) }
                .onFailure { AppLog.d(AREA, "Nothing can open the store page") }
        }
    }
}

/**
 * Google's in-app review card, requested ahead of time and launched when a note has been saved.
 * Google never says whether the card was shown, or what was rated; the app only knows how long the
 * flow took. It is never started from a button and nothing is drawn over it.
 */
class NativeReview internal constructor(
    private val manager: ReviewManager,
    private val info: Deferred<ReviewInfo?>
) {

    /**
     * Launches the card if Play handed it over within [waitMillis]; returns false when it did not.
     * That is no reason to ask again: the caller counts the attempt either way. [onFinished] runs
     * right after the card, with how long it took.
     */
    suspend fun launch(
        activity: Activity,
        waitMillis: Long,
        onFinished: (durationMillis: Long) -> Unit
    ): Boolean {
        val reviewInfo = withTimeoutOrNull(waitMillis) { info.await() } ?: return false
        if (activity.isFinishing || activity.isDestroyed) return false
        val startedAt = SystemClock.elapsedRealtime()
        try {
            manager.launchReview(activity, reviewInfo)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.d("Rating", "Review flow failed: ${e.javaClass.simpleName}")
        }
        onFinished(SystemClock.elapsedRealtime() - startedAt)
        return true
    }
}
