package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.prompt.PromptIds
import com.marfolog.noteswidgetformarkdown.util.prompt.PromptSurfaces

/**
 * What the rate chip remembers between process starts. The schedule itself is [RateChipSchedule];
 * this only reads and writes the timestamps and answers it needs.
 */
object RateReminder {

    /** Same key the Play build has written since 1.2.0, so existing users keep their date. */
    private const val KEY_FIRST_SEEN_AT = "rating_first_seen_at"
    private const val KEY_FEATURE_SEEN_AT = "rate_chip_feature_seen_at"
    private const val KEY_RATE_TAPPED = "rate_chip_rate_tapped"
    private const val KEY_NEVER_AGAIN = "rate_chip_never_again"
    private const val KEY_ENABLED = "rate_chip_enabled"
    private const val KEY_REPORTED_WINDOW = "rate_chip_reported_window"

    /**
     * Called on every process start, which widget updates cause too — so it also counts people who
     * never open the app itself. Returns true on the first start after updating from a build
     * without the chip.
     */
    fun recordStart(context: Context, nowMillis: Long): Boolean = recordStart(store(context), nowMillis)

    internal fun recordStart(store: KeyValueStore, nowMillis: Long): Boolean {
        val hadFirstSeen = store.contains(KEY_FIRST_SEEN_AT)
        val hadFeatureSeen = store.contains(KEY_FEATURE_SEEN_AT)
        if (!hadFirstSeen) store.putLong(KEY_FIRST_SEEN_AT, nowMillis)
        if (!hadFeatureSeen) store.putLong(KEY_FEATURE_SEEN_AT, nowMillis)
        return hadFirstSeen && !hadFeatureSeen
    }

    fun firstSeenAt(context: Context): Long? = firstSeenAt(store(context))

    internal fun firstSeenAt(store: KeyValueStore): Long? = store.timestamp(KEY_FIRST_SEEN_AT)

    fun openWindow(context: Context, nowMillis: Long): Int? = openWindow(store(context), nowMillis)

    internal fun openWindow(store: KeyValueStore, nowMillis: Long): Int? = RateChipSchedule.openWindow(
        nowMillis = nowMillis,
        firstSeenAtMillis = store.timestamp(KEY_FIRST_SEEN_AT),
        featureSeenAtMillis = store.timestamp(KEY_FEATURE_SEEN_AT),
        whatsNewShownAtMillis = WhatsNewReminder.shownAt(store),
        rateTapped = store.getBoolean(KEY_RATE_TAPPED, false),
        neverAgain = store.getBoolean(KEY_NEVER_AGAIN, false),
        disabledInSettings = !store.getBoolean(KEY_ENABLED, true)
    )

    fun isSettingAvailable(context: Context, nowMillis: Long): Boolean =
        isSettingAvailable(store(context), nowMillis)

    internal fun isSettingAvailable(store: KeyValueStore, nowMillis: Long): Boolean =
        RateChipSchedule.isSettingAvailable(
            nowMillis = nowMillis,
            firstSeenAtMillis = store.timestamp(KEY_FIRST_SEEN_AT),
            featureSeenAtMillis = store.timestamp(KEY_FEATURE_SEEN_AT),
            whatsNewShownAtMillis = WhatsNewReminder.shownAt(store)
        )

    /** Both widgets load their data on every refresh; the window is reported by whichever is first. */
    fun reportShownOnce(context: Context, window: Int?) {
        if (claimShownWindow(store(context), window)) {
            PromptSurfaces.recordShown(context, PromptIds.RATE_CHIP)
            Telemetry.event(UsageEvent.RATE_CHIP_SHOWN, UsageEvent.PARAM_WINDOW to window.toString())
        }
    }

    @Synchronized
    internal fun claimShownWindow(store: KeyValueStore, window: Int?): Boolean {
        val lastReported = store.getInt(KEY_REPORTED_WINDOW)?.takeIf { it >= 0 }
        if (window == null || !RateChipSchedule.isFirstShowOfWindow(window, lastReported)) return false
        store.putInt(KEY_REPORTED_WINDOW, window)
        return true
    }

    fun isEnabled(context: Context): Boolean = store(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) = store(context).putBoolean(KEY_ENABLED, enabled)

    fun markRateTapped(context: Context) = markRateTapped(store(context))

    internal fun markRateTapped(store: KeyValueStore) = store.putBoolean(KEY_RATE_TAPPED, true)

    /** Rate tapped anywhere: the chip, "What's new", settings or the dialog after a saved note. */
    internal fun rateTapped(store: KeyValueStore): Boolean = store.getBoolean(KEY_RATE_TAPPED, false)

    fun markNeverAgain(context: Context) = markNeverAgain(store(context))

    internal fun markNeverAgain(store: KeyValueStore) = store.putBoolean(KEY_NEVER_AGAIN, true)

    /** "Don't ask again" on the widget's chip, which every other request for a rating respects too. */
    internal fun neverAskedAgain(store: KeyValueStore): Boolean = store.getBoolean(KEY_NEVER_AGAIN, false)

    internal fun setEnabled(store: KeyValueStore, enabled: Boolean) = store.putBoolean(KEY_ENABLED, enabled)

    private fun store(context: Context): KeyValueStore = SharedPreferencesStore(
        context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
    )

    private fun KeyValueStore.timestamp(key: String): Long? = getLong(key)?.takeIf { it >= 0 }
}
