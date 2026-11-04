package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import com.marfolog.noteswidgetformarkdown.BuildConfig
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.presentation.widget.AddButtons

/** What the "What's new" dialog remembers between starts. The rules are in [WhatsNew]. */
object WhatsNewReminder {

    private const val KEY_LAST_VERSION = "last_version_seen"
    private const val KEY_UPDATED_TO = "whats_new_updated_to"
    private const val KEY_SEEN_VERSION = "whats_new_seen_version"
    private const val KEY_SHOWN_AT = "whats_new_shown_at"

    /** On every process start, before [RateReminder.recordStart] writes its first date. */
    fun recordStart(context: Context) = recordStart(store(context), BuildConfig.VERSION_NAME)

    internal fun recordStart(store: KeyValueStore, versionName: String) {
        val current = WhatsNew.release(versionName)
        val last = store.getString(KEY_LAST_VERSION)
        if (last == current) return
        val installedBefore = RateReminder.firstSeenAt(store) != null || store.contains(SetupActivity.KEY_NOTES_URI)
        if (WhatsNew.arrivedByUpdate(last, current, installedBefore)) store.putString(KEY_UPDATED_TO, current)
        store.putString(KEY_LAST_VERSION, current)
    }

    fun shouldShow(context: Context, setupComplete: Boolean, openedFrom: String?): Boolean =
        shouldShow(store(context), BuildConfig.VERSION_NAME, setupComplete, openedFrom, WhatsNewSwitch.isEnabled(context))

    internal fun shouldShow(
        store: KeyValueStore,
        versionName: String,
        setupComplete: Boolean,
        openedFrom: String?,
        remoteEnabled: Boolean
    ): Boolean = WhatsNew.shouldShow(
        versionName = versionName,
        updatedToVersion = store.getString(KEY_UPDATED_TO),
        seenVersion = store.getString(KEY_SEEN_VERSION),
        openedFrom = openedFrom,
        remoteEnabled = remoteEnabled,
        setupComplete = setupComplete
    )

    /** Whether the dialog starts with the add buttons question, which it must not lose by being left. */
    fun needsButtonsChoice(context: Context): Boolean =
        WhatsNew.needsButtonsChoice(BuildConfig.VERSION_NAME, AddButtons.choice(context))

    /**
     * Stored the moment it appears, so neither a crash nor a rotation can bring it back. When the
     * dialog starts with a question, the moment it has been answered instead.
     */
    fun markShown(context: Context, nowMillis: Long) = markShown(store(context), BuildConfig.VERSION_NAME, nowMillis)

    internal fun markShown(store: KeyValueStore, versionName: String, nowMillis: Long) {
        store.putString(KEY_SEEN_VERSION, WhatsNew.release(versionName))
        store.putLong(KEY_SHOWN_AT, nowMillis)
    }

    internal fun shownAt(store: KeyValueStore): Long? = store.getLong(KEY_SHOWN_AT)?.takeIf { it >= 0 }

    private fun store(context: Context): KeyValueStore = SharedPreferencesStore(
        context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
    )
}
