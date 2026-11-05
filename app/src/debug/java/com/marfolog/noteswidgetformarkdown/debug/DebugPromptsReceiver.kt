package com.marfolog.noteswidgetformarkdown.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.marfolog.noteswidgetformarkdown.presentation.widget.NotesWidget
import com.marfolog.noteswidgetformarkdown.util.AppClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug builds only, driven from adb:
 *
 *     am broadcast -p com.marfolog.noteswidgetformarkdown -a com.marfolog.noteswidgetformarkdown.debug.TIME --el offsetDays 7
 *     am broadcast -p com.marfolog.noteswidgetformarkdown -a com.marfolog.noteswidgetformarkdown.debug.TIME --el addDays 1
 *     am broadcast -p com.marfolog.noteswidgetformarkdown -a com.marfolog.noteswidgetformarkdown.debug.TIME --ez reset true
 *     am broadcast -p com.marfolog.noteswidgetformarkdown -a com.marfolog.noteswidgetformarkdown.debug.PROMPTS_RESET
 *     am broadcast -p com.marfolog.noteswidgetformarkdown -a com.marfolog.noteswidgetformarkdown.debug.PROMPTS_DUMP
 *
 * and the answer is in `logcat -s NWPrompts`. The release manifest does not list this receiver.
 */
class DebugPromptsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            ACTION_TIME -> {
                when {
                    intent.getBooleanExtra("reset", false) -> AppClock.reset(app)
                    // --el and --ei both work: a number is a number.
                    number(intent, "offsetDays") != null -> AppClock.setOffsetDays(app, number(intent, "offsetDays")!!)
                    number(intent, "addDays") != null -> AppClock.addDays(app, number(intent, "addDays")!!)
                    else -> Log.w(TAG, "TIME needs offsetDays, addDays or reset")
                }
            }
            ACTION_RESET -> DebugTools.resetPrompts(app)
            ACTION_DUMP -> Unit
            else -> return
        }
        DebugTools.report(app)
        // The rate chip depends on the day, so the widget is drawn again with the new one.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                NotesWidget.updateAll(app)
            } catch (t: Throwable) {
                Log.w(TAG, "Widget refresh failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    private fun number(intent: Intent, key: String): Long? = (intent.extras?.get(key) as? Number)?.toLong()

    private companion object {
        const val TAG = "NWPrompts"
        const val ACTION_TIME = "com.marfolog.noteswidgetformarkdown.debug.TIME"
        const val ACTION_RESET = "com.marfolog.noteswidgetformarkdown.debug.PROMPTS_RESET"
        const val ACTION_DUMP = "com.marfolog.noteswidgetformarkdown.debug.PROMPTS_DUMP"
    }
}
