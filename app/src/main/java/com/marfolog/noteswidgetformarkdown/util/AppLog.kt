package com.marfolog.noteswidgetformarkdown.util

import android.util.Log

/**
 * Every log line in the app goes through here, under one tag prefix.
 *
 * In the foss build nothing leaves the device, so a bug report is only as good as what the user can
 * read out of logcat themselves. One prefix makes that a single command:
 *
 * ```
 * adb logcat -s NW:V
 * ```
 *
 * Log what a reader of a bug report would need: whether a permission survived, why a file could not
 * be read. Never log note contents.
 *
 * In the Play build these lines are also attached to crash reports as breadcrumbs, so anything
 * written here can leave the device. Storage locations are stripped before that happens — see
 * [redactLocations] — because the privacy policy promises folder paths are never sent, and a vault
 * folder name is often personal. Logcat still shows them in full.
 */
object AppLog {

    private const val TAG = "NW"

    fun d(area: String, message: String) = Log.d(TAG, "[$area] $message")

    fun i(area: String, message: String) {
        Log.i(TAG, "[$area] $message")
        Telemetry.breadcrumb("[$area] ${redactLocations(message)}")
    }

    fun w(area: String, message: String, error: Throwable? = null) {
        if (error == null) Log.w(TAG, "[$area] $message") else Log.w(TAG, "[$area] $message", error)
        Telemetry.breadcrumb("[$area] ${redactLocations(message)}")
    }

    /** Errors are also recorded as non-fatals, so they surface without waiting for a crash. */
    fun e(area: String, message: String, error: Throwable? = null) {
        if (error == null) Log.e(TAG, "[$area] $message") else Log.e(TAG, "[$area] $message", error)
        Telemetry.breadcrumb("[$area] ${redactLocations(message)}")
        error?.let { Telemetry.recordError(redacted(it)) }
    }

    /**
     * A throwable's own message is not ours to trust. SAF failures put the whole tree URI in it,
     * which spells out the vault folder and the note file name, and that would reach the crash
     * reporter untouched. This keeps the type and the stack trace, which is what makes a report
     * useful, and rewrites only the text.
     */
    private fun redacted(error: Throwable): Throwable =
        RedactedThrowable(
            "${error.javaClass.name}: ${redactLocations(error.message.orEmpty())}",
            error.cause?.let { redacted(it) }
        ).also { it.stackTrace = error.stackTrace }

    /** Carries a cleaned message; the original type is kept in the text. */
    internal class RedactedThrowable(message: String, cause: Throwable?) : Throwable(message, cause)

    /** Folder URIs contain the full path, which is fine, but note file names are not logged. */
    fun redactFileName(uri: String): String = uri.substringBeforeLast("%2F", uri)

    /**
     * Replaces any storage URI with a placeholder before a line can be attached to a crash report.
     * A SAF tree URI spells out the vault folder name and its path, which is exactly what the
     * privacy policy says is never transmitted.
     */
    internal fun redactLocations(message: String): String =
        STORAGE_URI.replace(message, "<storage location>")

    // Also plain paths: providers and the framework hand those back in messages just as often.
    private val STORAGE_URI = Regex("""((content|file)://|/storage/|/sdcard/)\S*""")
}
