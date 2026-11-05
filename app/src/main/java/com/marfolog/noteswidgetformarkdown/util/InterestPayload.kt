package com.marfolog.noteswidgetformarkdown.util

/**
 * The one document the Play build sends when somebody taps "yes" or "no" on the question whether a
 * feature that does not exist yet would be used. Field names match `firestore.rules` in the
 * repository root; the server adds the time itself. Nothing else goes with it: no identifier of the
 * user, the device or the installation, no folder, no language.
 */
internal object InterestPayload {

    const val COLLECTION = "interest"
    const val FIELD_FEATURE = "feature"
    const val FIELD_ANSWER = "answer"
    const val FIELD_APP_VERSION = "appVersion"
    const val FIELD_VERSION_CODE = "versionCode"
    const val FIELD_CREATED_AT = "createdAt"

    /** Mirrors the list in `firestore.rules`: a feature the rules do not know is refused there. */
    val FEATURES = setOf(UsageEvent.FEATURE_MULTI_FOLDER_WIDGETS)

    /** Only a tap on "yes" or "no" is sent; closing the banner or ignoring it is not an answer to send. */
    val ANSWERS = setOf(UsageEvent.ANSWER_YES, UsageEvent.ANSWER_NO)

    fun isSendable(feature: String, answer: String): Boolean = feature in FEATURES && answer in ANSWERS

    /** [versionName] is cut to the 40 characters the rules allow, so a long one never fails the send. */
    fun of(feature: String, answer: String, versionName: String, versionCode: Int): Map<String, Any?> = mapOf(
        FIELD_FEATURE to feature,
        FIELD_ANSWER to answer,
        FIELD_APP_VERSION to versionName.take(40),
        FIELD_VERSION_CODE to versionCode
    )
}
