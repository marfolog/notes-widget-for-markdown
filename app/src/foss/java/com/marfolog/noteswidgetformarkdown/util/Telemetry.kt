package com.marfolog.noteswidgetformarkdown.util

import android.content.Context

/**
 * FOSS flavour: reports nothing, anywhere.
 *
 * Same API as the Play twin so the rest of the app is identical, but every call is a no-op. This
 * build declares no Android permissions at all — not even INTERNET — which is what F-Droid needs
 * and what the README's privacy claim rests on.
 */
object Telemetry {

    const val ENABLED = false

    fun init(context: Context) = Unit

    fun event(name: String, vararg params: Pair<String, String>) = Unit

    fun breadcrumb(message: String) = Unit

    fun recordError(error: Throwable) = Unit
}
