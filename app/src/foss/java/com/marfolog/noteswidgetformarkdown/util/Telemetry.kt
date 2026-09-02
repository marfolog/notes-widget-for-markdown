package com.marfolog.noteswidgetformarkdown.util

import android.content.Context

/**
 * FOSS flavour: reports nothing, anywhere.
 *
 * Same API as the Play twin so the rest of the app is identical, but every call is a no-op.
 * This build has no INTERNET permission, which CI checks on every release.
 */
object Telemetry {

    const val ENABLED = false

    fun init(context: Context) = Unit

    /** Nothing to switch off — this build reports nothing in the first place. */
    fun isEnabled(context: Context): Boolean = false

    fun setEnabled(context: Context, enabled: Boolean) = Unit

    /** Nothing is ever sent here, so there is nothing to ask about. */
    fun hasBeenAsked(context: Context): Boolean = true

    fun markAsked(context: Context) = Unit

    fun event(name: String, vararg params: Pair<String, String>) = Unit

    fun breadcrumb(message: String) = Unit

    fun recordError(error: Throwable) = Unit
}
