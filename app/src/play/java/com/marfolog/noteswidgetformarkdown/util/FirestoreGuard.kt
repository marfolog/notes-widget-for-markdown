package com.marfolog.noteswidgetformarkdown.util

/**
 * Whether the app may write to the real Firestore. A debug build never does: it is run to look at,
 * the analytics are off in it for the same reason, and a tap on "yes" or "Send" while testing must
 * not land among the answers of real people.
 */
internal object FirestoreGuard {
    fun mayWrite(debugBuild: Boolean): Boolean = !debugBuild
}
