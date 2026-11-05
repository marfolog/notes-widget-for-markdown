package com.marfolog.noteswidgetformarkdown.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        assertEquals("feature/sync", parser.parseBranch("ref: refs/heads/feature/sync"))
    }

    @Test
    fun `returns null branch for detached HEAD`() {
        assertNull(parser.parseBranch("9f1c2b3d4e5f60718293a4b5c6d7e8f901234567"))
    }

    @Test
    fun `a branch with a slash keeps its whole name`() {
        assertEquals("feature/x", parser.parseBranch("ref: refs/heads/feature/x\n"))
        assertEquals("team/alice/notes", parser.parseBranch("ref: refs/heads/team/alice/notes"))
    }

    @Test
    fun `lines keep both shas and the action exactly as written`() {
        val reflog = "$OLD $NEW Marfolog <m@example.com> 1793883720 +0100\tcommit (amend): vault: 2026-11-05 14:02\n"

        val line = parser.parseLines(reflog).single()

        assertEquals(OLD, line.oldSha)
        assertEquals(NEW, line.newSha)
        assertEquals(1793883720_000L, line.timestampMillis)
        assertEquals("commit (amend)", line.rawAction)
    }

    @Test
    fun `a name with spaces does not confuse the timestamp`() {
        val reflog = "$OLD $NEW Jane Q. Public <jq@example.com> 1793883720 +0100\tcommit: note\n"

        assertEquals(1793883720_000L, parser.parseLines(reflog).single().timestampMillis)
    }

    @Test
    fun `reading from the middle of a reflog drops the cut first line`() {
        val reflog = "e2b84d76a9f0 $NEW Marfolog <m@example.com> 1793883000 +0100\tcommit: cut\n" +
            "$OLD $NEW Marfolog <m@example.com> 1793883720 +0100\tcommit: whole\n"

        val lines = parser.parseLines(reflog, partialFirstLine = true)

        assertEquals(1, lines.size)
        assertEquals(1793883720_000L, lines.single().timestampMillis)
    }

    @Test
    fun `a whole first line is kept when nothing was cut`() {
        val reflog = "$OLD $NEW Marfolog <m@example.com> 1793883720 +0100\tcommit: first\n" +
            "$NEW $OLD Marfolog <m@example.com> 1793883780 +0100\tcommit: second\n"

        assertEquals(2, parser.parseLines(reflog, partialFirstLine = false).size)
        assertEquals(1, parser.parseLines(reflog, partialFirstLine = true).size)
    }

    @Test
    fun `pull, fast-forward and clone count as contact with the remote`() {
        assertTrue(parser.isRemoteContact("pull"))
        assertTrue(parser.isRemoteContact("pull --rebase (finish)"))
        assertTrue(parser.isRemoteContact("Fast-Forward"))
        assertTrue(parser.isRemoteContact("clone"))
    }

    @Test
    fun `commit, checkout and reset do not count as contact with the remote`() {
        assertFalse(parser.isRemoteContact("commit"))
        assertFalse(parser.isRemoteContact("commit (amend)"))
        assertFalse(parser.isRemoteContact("checkout"))
        assertFalse(parser.isRemoteContact("reset"))
        assertFalse(parser.isRemoteContact(""))
    }

    private companion object {
        const val OLD = "dae225cb0b0c4e0e8a1d6f7b5c3a29e41f8d7b60"
        const val NEW = "5a1c0f93e2b84d76a9f0c3d1e8b7a6254c9d0e13"
    }
}
