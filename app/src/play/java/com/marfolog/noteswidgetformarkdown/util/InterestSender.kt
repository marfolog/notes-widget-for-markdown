package com.marfolog.noteswidgetformarkdown.util

import com.google.firebase.firestore.FieldValue
import com.marfolog.noteswidgetformarkdown.BuildConfig

/**
 * Play flavour: stores one answer to the question whether a feature would be used as a Firestore
 * document, and nothing else. Only ever called from the tap on "yes" or "no", so it does not depend
 * on the analytics switch, in the same way the feedback form does not. The foss twin sends nothing,
 * and so does a debug build, which only logs what it would have sent.
 *
 * A failure is only written to the log: it is not worth an error on screen, and a send that cannot
 * go out right now is queued by Firestore until the process ends.
 */
internal object InterestSender {

    private const val AREA = "Interest"

    fun send(feature: String, answer: String) {
        deliver(feature, answer, BuildConfig.DEBUG) { document ->
            PlayFirestore.instance.collection(InterestPayload.COLLECTION).add(document)
                .addOnFailureListener { AppLog.d(AREA, "Answer not stored: ${it.javaClass.simpleName}") }
        }
    }

    /** [store] is what writes the document; it is never called for a debug build. Returns whether it was. */
    internal fun deliver(
        feature: String,
        answer: String,
        debugBuild: Boolean,
        log: (String) -> Unit = { AppLog.i(AREA, it) },
        store: (Map<String, Any?>) -> Unit
    ): Boolean {
        if (!InterestPayload.isSendable(feature, answer)) return false
        val document = InterestPayload.of(feature, answer, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        if (!FirestoreGuard.mayWrite(debugBuild)) {
            log("Debug build, would send to Firestore: $document")
            return false
        }
        runCatching { store(document + (InterestPayload.FIELD_CREATED_AT to FieldValue.serverTimestamp())) }
            .onFailure { log("Answer not sent: ${it.javaClass.simpleName}") }
        return true
    }
}
