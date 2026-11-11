package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import android.os.Bundle
import android.util.Log
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

    private const val PREFS = "app_prefs"
    private const val KEY_ENABLED = "telemetry_enabled"
    private const val KEY_ASKED = "telemetry_asked"

    private var analytics: FirebaseAnalytics? = null
    @Volatile
    private var appContext: Context? = null

    /**
     * Collection starts switched off in the manifest, so nothing leaves the device before this
     * runs. The user's answer then switches it on or keeps it off.
     */
    fun init(context: Context) {
        if (appContext == null) installRedactingCrashHandler()
        appContext = context.applicationContext
        analytics = FirebaseAnalytics.getInstance(context)
        // No ad identifier: the AD_ID permission is stripped in this flavour's manifest.
        analytics?.setConsent(
            mapOf(
                FirebaseAnalytics.ConsentType.AD_STORAGE to FirebaseAnalytics.ConsentStatus.DENIED,
                FirebaseAnalytics.ConsentType.AD_USER_DATA to FirebaseAnalytics.ConsentStatus.DENIED,
                FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.DENIED,
                FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to FirebaseAnalytics.ConsentStatus.GRANTED
            )
        )
        // Last, so the user's choice is what survives. An earlier version set collection to true
        // right after reading it, which turned every restart into a silent opt-in again.
        applyOptOut(context, isEnabled(context))
    }

    /**
     * Off until the user says otherwise. Firebase writes an identifier to the device, and for that
     * ePrivacy asks for consent rather than a balancing test — so nothing is sent before the
     * question settings ask once the widget is set up is answered with yes.
     */
    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    /** Whether the reporting question has been answered, either way. */
    fun hasBeenAsked(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ASKED, false)

    fun markAsked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ASKED, true).apply()
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
        applyOptOut(context, enabled)
    }

    private fun applyOptOut(context: Context, enabled: Boolean) {
        analytics?.setAnalyticsCollectionEnabled(enabled)
        FirebaseCrashlytics.getInstance().apply {
            isCrashlyticsCollectionEnabled = enabled
            if (!enabled) deleteUnsentReports()
        }
    }

    fun event(name: String, vararg params: Pair<String, String>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) -> putString(key, value) }
        }
        // Debug builds never send events (collection stays off), so this is how a test run sees them.
        // Names and values are the fixed words of UsageEvent, never note content.
        val debuggable = appContext?.let { it.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0 }
        if (debuggable == true) AppLog.d("Event", "$name ${params.joinToString(" ") { "${it.first}=${it.second}" }}")
        analytics?.logEvent(name, bundle)
    }

    fun setUserProperty(name: String, value: String) {
        analytics?.setUserProperty(name, value)
    }

    /** Breadcrumbs, so a crash report shows the last few steps that led to it. */
    fun breadcrumb(message: String) {
        FirebaseCrashlytics.getInstance().log(CrashRedactor.message(message))
    }

    fun recordError(error: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(CrashRedactor.redact(error))
    }

    /**
     * Crashlytics records a crash from its own uncaught-exception handler, message and all. This
     * one sits in front of it and hands it a cleaned copy instead; the app still crashes as before.
     * Logcat keeps the original, which never leaves the phone.
     */
    private fun installRedactingCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler() ?: return
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            val reporting = appContext?.let(::isEnabled) == true
            if (reporting) Log.e(CRASH_TAG, "Uncaught exception in ${thread.name}", error)
            CrashRedactor.forward(previous, thread, error)
        }
    }

    private const val CRASH_TAG = "NW"
}
