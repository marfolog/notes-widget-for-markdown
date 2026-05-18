package com.marfolog.noteswidgetformarkdown.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MarkdownPreviewFormatterTest {

    private val formatter = MarkdownPreviewFormatter()

    @Test
    fun formatsTaskListItemsWithoutRawCheckboxSyntax() {
        val preview = formatter.format(
            """
            - [ ] Buy milk
            - [x] Pay bills
            """.trimIndent()
        )

        assertEquals(
            """
            Todo: Buy milk
            Done: Pay bills
            """.trimIndent(),
            preview
        )
        assertFalse(preview.contains("[ ]"))
        assertFalse(preview.contains("[x]"))
    }

    @Test
    fun removesCommonMarkdownSyntaxFromPreview() {
        val preview = formatter.format(
            """
            # Project Notes
            This is **important** and _useful_.
            Open [Obsidian](https://obsidian.md) and `sync`.
            ~~Old idea~~
            """.trimIndent()
        )

        assertEquals(
            """
            Project Notes
            This is important and useful.
            Open Obsidian and sync.
            Old idea
            """.trimIndent(),
            preview
        )
    }

    @Test
    fun normalizesObsidianWikiLinksAndCallouts() {
        val preview = formatter.format(
            """
            ---
            tag: test
            ---
            > [!NOTE] Remember [[Projects/App|the app]]
            Link to [[Daily Note]]
            """.trimIndent()
        )

        assertEquals(
            """
            Remember the app
            Link to Daily Note
            """.trimIndent(),
            preview
        )
    }

    @Test
    fun limitsPreviewLines() {
        val preview = MarkdownPreviewFormatter(previewLineCount = 5).format(
            """
            One
            Two
            Three
            Four
            Five
            Six
            """.trimIndent()
        )

        assertEquals(
            """
            One
            Two
            Three
            Four
            Five
            """.trimIndent(),
            preview
        )
    }
}
