package com.marfolog.noteswidgetformarkdown.presentation.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class NoteTimeLabelTest {

    private val now = Calendar.getInstance().apply {
        set(2026, Calendar.SEPTEMBER, 1, 16, 30, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun `today shows the time`() {
        val earlierToday = now - 2 * 60 * 60 * 1000L

        assertEquals("14:30", NoteTimeLabel.format(earlierToday, now))
    }

    @Test
    fun `yesterday is named, not dated`() {
        val yesterday = now - 24 * 60 * 60 * 1000L

        assertEquals("yesterday", NoteTimeLabel.format(yesterday, now))
    }

    @Test
    fun `older dates within the year omit the year`() {
        val lastMonth = now - 30L * 24 * 60 * 60 * 1000L

        val label = NoteTimeLabel.format(lastMonth, now)

        assertTrue(label.isNotEmpty())
        assertTrue("expected no year in \"$label\"", !label.contains("2026"))
    }

    @Test
    fun `previous years keep the year`() {
        val lastYear = Calendar.getInstance().apply {
            set(2025, Calendar.MARCH, 4, 9, 0, 0)
        }.timeInMillis

        assertTrue(NoteTimeLabel.format(lastYear, now).contains("2025"))
    }

    @Test
    fun `missing timestamp renders nothing`() {
        assertEquals("", NoteTimeLabel.format(0L, now))
    }
}
