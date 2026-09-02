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
    fun `empty file becomes a single bullet`() {
        assertEquals("- First thought\n", inserter.insert("", "First thought"))
    }

    @Test
    fun `file with only frontmatter gets the bullet after it, not inside it`() {
        val updated = inserter.insert(
            """
            ---
            tags:
              - inbox
            ---
            """.trimIndent(),
            "New idea"
        )

        assertEquals(
            """
            ---
            tags:
              - inbox
            ---
            - New idea
            """.trimIndent(),
            updated
        )
    }

    @Test
    fun `unclosed frontmatter is treated as plain content`() {
        // A half-written frontmatter block has no safe insert point inside it; falling back
        // to the top of the file must not corrupt the YAML any further.
        val updated = inserter.insert("---\ntags: broken", "New idea")

        assertEquals("- New idea\n---\ntags: broken", updated)
    }

    @Test
    fun `heading-like line inside frontmatter does not attract the bullet`() {
        val updated = inserter.insert(
            """
            ---
            # a yaml comment, not a heading
            ---
            Existing text
            """.trimIndent(),
            "New idea"
        )

        assertEquals(
            """
            ---
            # a yaml comment, not a heading
            ---
            - New idea
            Existing text
            """.trimIndent(),
            updated
        )
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
