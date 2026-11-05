package com.marfolog.noteswidgetformarkdown.util

import android.content.Context

/**
 * The time every decision about prompts is made by. In the release build it is the system clock and
 * nothing else: the debug build has a twin of this file that can move it, for testing.
 */
object AppClock {
    fun init(context: Context) = Unit

    fun now(): Long = System.currentTimeMillis()
}
