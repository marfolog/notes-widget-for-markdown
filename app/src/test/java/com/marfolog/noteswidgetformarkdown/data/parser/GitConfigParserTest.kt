package com.marfolog.noteswidgetformarkdown.data.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class GitConfigParserTest {

    private val parser = GitConfigParser()

    private val config = """
        [core]
        	repositoryformatversion = 0
        	remote = not-this-one
        [remote "origin"]
        	url = https://git.example.com/me/vault.git
        [remote "backup"]
        	url = https://backup.example.com/me/vault.git
        [branch "main"]
        	remote = origin
        	merge = refs/heads/main
        [branch "feature/x"]
        	merge = refs/heads/feature/x
        	remote = backup
    """.trimIndent()

    @Test
    fun `a branch tracking another remote reads that remote`() {
        assertEquals("backup", parser.remoteFor(config, "feature/x"))
    }

    @Test
    fun `a branch tracking origin reads origin`() {
        assertEquals("origin", parser.remoteFor(config, "main"))
    }

    @Test
    fun `a branch without a section falls back to origin`() {
        assertEquals("origin", parser.remoteFor(config, "draft"))
    }

    @Test
    fun `a remote key outside the branch section is ignored`() {
        val onlyCore = "[core]\n\tremote = elsewhere\n[branch \"main\"]\n\tmerge = refs/heads/main\n"

        assertEquals("origin", parser.remoteFor(onlyCore, "main"))
    }

    @Test
    fun `an empty remote value falls back to origin`() {
        assertEquals("origin", parser.remoteFor("[branch \"main\"]\n\tremote =\n", "main"))
    }

    @Test
    fun `an empty config falls back to origin`() {
        assertEquals("origin", parser.remoteFor("", "main"))
    }

    @Test
    fun `a branch name only matches whole, not as a prefix`() {
        val prefix = "[branch \"main-old\"]\n\tremote = backup\n"

        assertEquals("origin", parser.remoteFor(prefix, "main"))
    }
}
