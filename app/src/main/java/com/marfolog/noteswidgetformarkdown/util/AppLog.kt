package com.marfolog.noteswidgetformarkdown.util

import android.util.Log

/**
 * Every log line in the app goes through here, under one tag prefix.
 *
 * The app sends nothing anywhere — no analytics, no crash reporting — so a bug report is only ever
 * as good as what the user can read out of logcat themselves. One prefix makes that a single
 * command:
 *
 * ```
 * adb logcat -s NW:V
 * ```
 *
 * Log what a reader of a bug report would need: which folder was picked, whether a permission
 * survived, why a file could not be read. Never log note contents.
 */
object AppLog {

    private const val TAG = "NW"

    fun d(area: String, message: String) = Log.d(TAG, "[$area] $message")

    fun i(area: String, message: String) {
        Log.i(TAG, "[$area] $message")
        Telemetry.breadcrumb("[$area] $message")
    }

    fun w(area: String, message: String, error: Throwable? = null) {
        if (error == null) Log.w(TAG, "[$area] $message") else Log.w(TAG, "[$area] $message", error)
        Telemetry.breadcrumb("[$area] $message")
    }

    /** Errors are also recorded as non-fatals, so they surface without waiting for a crash. */
    fun e(area: String, message: String, error: Throwable? = null) {
        if (error == null) Log.e(TAG, "[$area] $message") else Log.e(TAG, "[$area] $message", error)
        Telemetry.breadcrumb("[$area] $message")
        error?.let { Telemetry.recordError(it) }
    }

    /** Folder URIs contain the full path, which is fine, but note file names are not logged. */
    fun redactFileName(uri: String): String = uri.substringBeforeLast("%2F", uri)
}
