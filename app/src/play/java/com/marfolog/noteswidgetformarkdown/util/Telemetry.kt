package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Play flavour: reports usage and crashes to Firebase.
 *
 * This is the only place in the app that talks to the network, and it exists because installs
 * outside Google Play are invisible in Play Console. The foss flavour ships a no-op twin of this
 * file, so nothing outside this folder knows Firebase exists.
 *
 * Note titles, file names and note contents are never sent — only which action happened.
 */
object Telemetry {

    const val ENABLED = true

    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context)
        // The audience for this app does not expect ad tracking, and it buys nothing here.
        analytics?.setAnalyticsCollectionEnabled(true)
        // No ad identifier: the AD_ID permission is stripped in this flavour's manifest.
        analytics?.setConsent(
            mapOf(
                FirebaseAnalytics.ConsentType.AD_STORAGE to FirebaseAnalytics.ConsentStatus.DENIED,
                FirebaseAnalytics.ConsentType.AD_USER_DATA to FirebaseAnalytics.ConsentStatus.DENIED,
                FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.DENIED,
                FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to FirebaseAnalytics.ConsentStatus.GRANTED
            )
        )
    }

    fun event(name: String, vararg params: Pair<String, String>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) -> putString(key, value) }
        }
        analytics?.logEvent(name, bundle)
    }

    /** Breadcrumbs, so a crash report shows the last few steps that led to it. */
    fun breadcrumb(message: String) {
        FirebaseCrashlytics.getInstance().log(message)
    }

    fun recordError(error: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(error)
    }
}
