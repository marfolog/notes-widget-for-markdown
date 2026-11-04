package com.marfolog.noteswidgetformarkdown.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import com.marfolog.noteswidgetformarkdown.R
import kotlinx.coroutines.CoroutineScope

/**
 * FOSS flavour: there is no store here, so a rating is a star on GitHub.
 *
 * Same API as the Play twin. The rate chip points at the GitHub repository instead, where a star
 * does for this build what a rating does on Google Play.
 */
object RatingPrompt {

    const val NATIVE_REVIEW_AVAILABLE = false

    @StringRes
    val AFTER_NOTE_TITLE_RES = R.string.after_note_rating_title_foss

    @StringRes
    val RATE_BODY_RES = R.string.rate_dialog_body_foss

    @StringRes
    val RATE_BUTTON_RES = R.string.rate_dialog_rate_foss

    @StringRes
    val WHATS_NEW_FOOTER_RES = R.string.whats_new_footer_foss

    /** The row in the settings feedback section, where an icon already stands in for the star. */
    @StringRes
    val ABOUT_RATE_RES = R.string.about_star

    fun openRatePage(context: Context) = ProjectLinks.open(context, ProjectLinks.REPO_URL)

    fun ratePageIntent(context: Context): Intent = ProjectLinks.viewIntent(ProjectLinks.REPO_URL)

    fun prepareNativeReview(activity: Activity, scope: CoroutineScope): NativeReview = NativeReview()

    fun openStorePage(context: Context) = Unit
}

/** FOSS flavour: there is no Google card here, so it is never launched. */
class NativeReview {

    suspend fun launch(
        activity: Activity,
        waitMillis: Long,
        onFinished: (durationMillis: Long) -> Unit
    ): Boolean = false
}
