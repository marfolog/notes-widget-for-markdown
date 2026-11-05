package com.marfolog.noteswidgetformarkdown.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterestPayloadTest {

    private val feature = UsageEvent.FEATURE_MULTI_FOLDER_WIDGETS

    @Test
    fun `the document has exactly the five keys the rules allow, the server adds the time`() {
        val document = InterestPayload.of(feature, "yes", "1.5.0", 9)
        assertEquals(setOf("feature", "answer", "appVersion", "versionCode"), document.keys)
        assertEquals(
            mapOf("feature" to "multi_folder_widgets", "answer" to "yes", "appVersion" to "1.5.0", "versionCode" to 9),
            document
        )
        assertEquals("createdAt", InterestPayload.FIELD_CREATED_AT)
        assertEquals("interest", InterestPayload.COLLECTION)
    }

    @Test
    fun `a version name longer than the rules allow is cut, never refused`() {
        val long = "1.5.0-" + "x".repeat(60)
        assertEquals(40, (InterestPayload.of(feature, "no", long, 9)["appVersion"] as String).length)
    }

    @Test
    fun `only a yes or no is sent, the close button and an ignored banner are not answers to send`() {
        assertTrue(InterestPayload.isSendable(feature, UsageEvent.ANSWER_YES))
        assertTrue(InterestPayload.isSendable(feature, UsageEvent.ANSWER_NO))
        assertFalse(InterestPayload.isSendable(feature, UsageEvent.ANSWER_DISMISSED))
        assertFalse(InterestPayload.isSendable(feature, UsageEvent.ANSWER_IGNORED))
    }

    @Test
    fun `a feature the rules do not know is not sent`() {
        assertFalse(InterestPayload.isSendable("something_else", "yes"))
    }

    @Test
    fun `nothing that identifies anybody is in the document`() {
        val forbidden = setOf("deviceId", "installId", "userId", "folder", "language", "locale", "email")
        assertTrue(InterestPayload.of(feature, "yes", "1.5.0", 9).keys.none { it in forbidden })
    }

    // region once per installation and feature

    @Test
    fun `a yes is sent once and remembered`() {
        val store = InMemoryKeyValueStore()
        val sent = mutableListOf<Pair<String, String>>()
        assertTrue(FeatureInterest.sendOnce(store, feature, "yes", asked = true) { f, a -> sent += f to a })
        assertEquals(listOf(feature to "yes"), sent)
        assertEquals(true, store.getBoolean("feature_interest_sent_$feature", false))
    }

    @Test
    fun `a second answer on the same installation sends nothing, whichever it is`() {
        val store = InMemoryKeyValueStore()
        val sent = mutableListOf<String>()
        FeatureInterest.sendOnce(store, feature, "no", asked = true) { _, a -> sent += a }
        assertFalse(FeatureInterest.sendOnce(store, feature, "yes", asked = true) { _, a -> sent += a })
        assertEquals(listOf("no"), sent)
    }

    @Test
    fun `closing the banner sends nothing and does not use up the one send`() {
        val store = InMemoryKeyValueStore()
        var calls = 0
        assertFalse(FeatureInterest.sendOnce(store, feature, UsageEvent.ANSWER_DISMISSED, asked = true) { _, _ -> calls++ })
        assertEquals(0, calls)
        assertFalse(store.contains("feature_interest_sent_$feature"))
        assertTrue(FeatureInterest.sendOnce(store, feature, "yes", asked = true) { _, _ -> calls++ })
        assertEquals(1, calls)
    }

    @Test
    fun `a build that does not ask sends nothing`() {
        val store = InMemoryKeyValueStore()
        var calls = 0
        assertFalse(FeatureInterest.sendOnce(store, feature, "yes", asked = false) { _, _ -> calls++ })
        assertEquals(0, calls)
    }

    @Test
    fun `sending does not depend on the analytics switch, only on the tap`() {
        // The decision has no input about consent at all: the same call is made with or without it.
        assertTrue(FeatureInterest.shouldSend(feature, "yes", alreadySent = false))
        assertFalse(FeatureInterest.shouldSend(feature, "yes", alreadySent = true))
    }

    // endregion
}
