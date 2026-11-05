package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity

/**
 * How many times settings were opened since this build came onto the phone, by install or by
 * update. Kept on its own so anything that wants to wait for "a few visits" reads one number.
 * A turn of the screen is not a new visit, which is why the caller counts only a fresh start.
 */
object SettingsVisits {

    private const val KEY_COUNT = "settings_visits"

    fun record(context: Context): Int = record(store(context))

    internal fun record(store: KeyValueStore): Int {
        val next = (store.getInt(KEY_COUNT) ?: 0).coerceAtLeast(0) + 1
        store.putInt(KEY_COUNT, next)
        return next
    }

    fun count(context: Context): Int = count(store(context))

    internal fun count(store: KeyValueStore): Int = (store.getInt(KEY_COUNT) ?: 0).coerceAtLeast(0)

    private fun store(context: Context): KeyValueStore = SharedPreferencesStore(
        context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
    )
}
