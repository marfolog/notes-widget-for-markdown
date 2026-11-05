package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import java.util.concurrent.TimeUnit

/**
 * Debug build only: the time every decision about prompts is made by, the system clock plus an
 * offset that can be moved to try "a week later" without waiting. The offset is kept in a file of
 * its own and moves nothing else: no file time, nothing that is sent anywhere.
 */
object AppClock {
    private const val PREFS = "debug_clock"
    private const val KEY_OFFSET = "offset_millis"

    @Volatile
    private var offsetMillis = 0L

    @Volatile
    private var loaded = false

    fun init(context: Context) {
        offsetMillis = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_OFFSET, 0L)
        loaded = true
    }

    fun now(): Long = System.currentTimeMillis() + offsetMillis

    fun offsetDays(): Long = TimeUnit.MILLISECONDS.toDays(offsetMillis)

    fun setOffsetDays(context: Context, days: Long) = setOffset(context, TimeUnit.DAYS.toMillis(days))

    fun addDays(context: Context, days: Long) = setOffset(context, offsetMillis + TimeUnit.DAYS.toMillis(days))

    fun reset(context: Context) = setOffset(context, 0L)

    private fun setOffset(context: Context, millis: Long) {
        offsetMillis = millis
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_OFFSET, millis).apply()
    }
}
