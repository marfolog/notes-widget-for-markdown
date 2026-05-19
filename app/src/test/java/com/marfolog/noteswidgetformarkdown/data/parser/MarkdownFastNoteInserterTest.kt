package com.marfolog.noteswidgetformarkdown.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownFastNoteInserterTest {

    private val inserter = MarkdownFastNoteInserter()

    @Test
    fun insertsBulletBelowFirstTopLevelHeading() {
        val updated = inserter.insert(
            """
            # Inbox
            Existing text
            """.trimIndent(),
            "Buy milk"
        )

        assertEquals(
            """
            # Inbox
            - Buy milk
            Existing text
            """.trimIndent(),
            updated
        )
    }

    @Test
    fun insertsBulletAfterFrontmatterAndHeading() {
        val updated = inserter.insert(
            """
            ---
            tags:
              - inbox
            ---
            # Inbox
            Existing text
            """.trimIndent(),
            "Call bank"
        )

        assertEquals(
            """
            ---
            tags:
              - inbox
            ---
            # Inbox
            - Call bank
            Existing text
            """.trimIndent(),
            updated
        )
    }

    @Test
    fun insertsBulletAtTopWhenHeadingIsMissing() {
        val updated = inserter.insert(
            """
            Existing text
            - Existing item
            """.trimIndent(),
            "New idea"
        )

        assertEquals(
            """
            - New idea
            Existing text
            - Existing item
            """.trimIndent(),
            updated
        )
    }

    @Test
    fun trimsNoteTextBeforeInserting() {
        val updated = inserter.insert("# Inbox", "  Follow up  ")

        assertEquals(
            """
            # Inbox
            - Follow up
            """.trimIndent(),
            updated
        )
    }

    @Test
    fun rejectsBlankNoteText() {
        val result = runCatching {
            inserter.insert("# Inbox", "   ")
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun normalizesLineEndings() {
        val updated = inserter.insert("# Inbox\r\nExisting text", "Fast note")

        assertEquals(
            """
            # Inbox
            - Fast note
            Existing text
            """.trimIndent(),
            updated
        )
    }
}
