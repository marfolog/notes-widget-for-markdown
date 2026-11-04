package com.marfolog.noteswidgetformarkdown.presentation.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class NewNoteLaunchTest {

    @Test
    fun `without a rate only the editor opens`() {
        assertEquals(listOf("editor"), newNoteLaunchOrder(rated = false, editor = "editor", ratePage = "store"))
    }

    @Test
    fun `after a rate the store is started last, so it sits over the editor and back leads to the editor`() {
        val order = newNoteLaunchOrder(rated = true, editor = "editor", ratePage = "store")

        assertEquals(listOf("editor", "store"), order)
        assertEquals("store", order.last())
    }

    @Test
    fun `the editor is part of every launch, whatever the answer`() {
        listOf(true, false).forEach { rated ->
            assertEquals("editor", newNoteLaunchOrder(rated, "editor", "store").first())
        }
    }
}
