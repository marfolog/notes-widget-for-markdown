package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.marfolog.noteswidgetformarkdown.R

/**
 * Links out of the app. A browser opens them, so this works in the foss build too, which has no
 * internet permission of its own.
 */
object ProjectLinks {

    const val REPO_URL = "https://github.com/marfolog/notes-widget-for-markdown"
    const val ISSUES_URL = "$REPO_URL/issues/new/choose"
    const val DISCUSSIONS_URL = "$REPO_URL/discussions"
    const val NEW_IDEA_URL = "$REPO_URL/discussions/new?category=ideas"
    const val PRIVACY_URL = "https://folmbuild.cz/notes-widget-privacy/"

    /** For an activity stack; no new task, so it sits on top of what was started before it. */
    fun viewIntent(url: String): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

    fun open(context: Context, url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            Toast.makeText(context, R.string.about_no_browser, Toast.LENGTH_SHORT).show()
        }
    }
}
