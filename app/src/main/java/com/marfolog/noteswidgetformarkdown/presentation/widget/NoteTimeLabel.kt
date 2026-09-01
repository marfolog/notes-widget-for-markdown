package com.marfolog.noteswidgetformarkdown.presentation.widget

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Short "when was this last touched" label for the list widget. Pure logic so the wording can be
 * unit tested without waiting for a real clock.
 */
object NoteTimeLabel {

    fun format(lastModifiedMillis: Long, nowMillis: Long): String {
        if (lastModifiedMillis <= 0L) {
            return ""
        }

        val then = Calendar.getInstance().apply { timeInMillis = lastModifiedMillis }
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }

        return when {
            isSameDay(then, now) -> TIME_FORMAT.format(Date(lastModifiedMillis))
            isYesterday(then, now) -> "yesterday"
            then.get(Calendar.YEAR) == now.get(Calendar.YEAR) ->
                DAY_FORMAT.format(Date(lastModifiedMillis))
            else -> FULL_DATE_FORMAT.format(Date(lastModifiedMillis))
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun isYesterday(then: Calendar, now: Calendar): Boolean {
        val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        return isSameDay(then, yesterday)
    }

    private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val DAY_FORMAT = SimpleDateFormat("d MMM", Locale.getDefault())
    private val FULL_DATE_FORMAT = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
}
