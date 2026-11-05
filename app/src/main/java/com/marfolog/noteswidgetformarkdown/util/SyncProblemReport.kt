package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.presentation.widget.GitSyncLabel

/**
 * Counts how often the sync chip leaves healthy: once when it enters a trouble state, not on every
 * widget refresh. Recovering forgets the state, so the next trouble counts again.
 */
object SyncProblemReport {

    private const val KEY_REPORTED_TYPE = "sync_problem_reported_type"

    fun reportOnce(context: Context, trouble: GitSyncLabel.Trouble?) {
        val store = SharedPreferencesStore(
            context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
        )
        val type = trouble?.analyticsType
        if (claim(store, type) && type != null) {
            Telemetry.event(UsageEvent.SYNC_PROBLEM_SHOWN, UsageEvent.PARAM_TYPE to type)
        }
    }

    @Synchronized
    internal fun claim(store: KeyValueStore, type: String?): Boolean {
        val last = store.getString(KEY_REPORTED_TYPE)
        if (type == null) {
            if (last != null) store.remove(KEY_REPORTED_TYPE)
            return false
        }
        if (type == last) return false
        store.putString(KEY_REPORTED_TYPE, type)
        return true
    }
}
