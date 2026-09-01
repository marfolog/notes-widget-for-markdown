package com.marfolog.noteswidgetformarkdown.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GitReflogParserTest {

    private val parser = GitReflogParser()

    @Test
    fun `parses last entry of a multi line reflog`() {
        val reflog = buildString {
            appendLine("0000000000000000000000000000000000000000 aaaa Marfo <m@example.com> 1735689600 +0100\tclone: from github.com")
            appendLine("aaaa bbbb Marfo <m@example.com> 1735776000 +0100\tcommit: vault sync")
            appendLine("bbbb cccc Marfo <m@example.com> 1735862400 +0100\tpull: Fast-forward")
        }

        val entry = parser.parseLastEntry(reflog)

        assertEquals(1735862400_000L, entry?.timestampMillis)
        assertEquals("pull", entry?.action)
    }

    @Test
    fun `ignores trailing blank lines`() {
        val reflog = "aaaa bbbb Marfo <m@example.com> 1735776000 +0100\tcommit: note\n\n\n"

        assertEquals("commit", parser.parseLastEntry(reflog)?.action)
    }

    @Test
    fun `returns null for empty or malformed reflog`() {
        assertNull(parser.parseLastEntry(""))
        assertNull(parser.parseLastEntry("not a reflog line"))
    }

    @Test
    fun `normalizes obsidian-git wording to pull`() {
        val reflog = "aaaa bbbb Marfolog <m@example.com> 1788264228 +0200\t" +
            "Fast-Forward: Setting refs/heads/main to id: aa9440743a873c07a8b9706b3514cafca160708a"

        assertEquals("pull", parser.parseLastEntry(reflog)?.action)
    }

    @Test
    fun `parses branch from HEAD`() {
        assertEquals("main", parser.parseBranch("ref: refs/heads/main\n"))
        assertEquals("sync", parser.parseBranch("ref: refs/heads/feature/sync"))
    }

    @Test
    fun `returns null branch for detached HEAD`() {
        assertNull(parser.parseBranch("9f1c2b3d4e5f60718293a4b5c6d7e8f901234567"))
    }
}
