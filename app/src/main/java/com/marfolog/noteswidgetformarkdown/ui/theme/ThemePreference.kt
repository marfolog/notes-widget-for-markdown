package com.marfolog.noteswidgetformarkdown.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Svetly nebo tmavy vzhled appky. Widget si barvy nese sam, tohle se ho netyka.
 *
 * Drzi se v pameti procesu jako Compose stav, aby prepnuti bylo videt okamzite, a zaroven
 * v SharedPreferences, at prezije restart.
 */
object ThemePreference {

    enum class Mode { System, Light, Dark }

    private const val PREFS_NAME = "app_prefs"
    private const val KEY = "theme_mode"

    var mode by mutableStateOf(Mode.System)
        private set

    fun load(context: Context) {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY, null)
        mode = Mode.entries.firstOrNull { it.name == stored } ?: Mode.System
    }

    fun set(context: Context, value: Mode) {
        mode = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, value.name)
            .apply()
    }
}
