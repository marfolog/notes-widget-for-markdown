package com.marfolog.noteswidgetformarkdown.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitPackedRefsParserTest {

    private val parser = GitPackedRefsParser()

    @Test
    fun `parses local and remote refs`() {
        val packed = """
            # pack-refs with: peeled fully-peeled sorted
            8cafb2f113d13d06f9d1229f2d5b9c116bfbb79a refs/heads/main
            aa9440743a873c07a8b9706b3514cafca160708a refs/remotes/origin/main
        """.trimIndent()

        val refs = parser.parse(packed)

        assertEquals("8cafb2f113d13d06f9d1229f2d5b9c116bfbb79a", refs["refs/heads/main"])
        assertEquals("aa9440743a873c07a8b9706b3514cafca160708a", refs["refs/remotes/origin/main"])
    }

    @Test
    fun `skips header and peeled tag lines`() {
        val packed = """
            # pack-refs with: peeled fully-peeled sorted
            1111111111111111111111111111111111111111 refs/tags/v1
            ^2222222222222222222222222222222222222222
        """.trimIndent()

        val refs = parser.parse(packed)

        assertEquals(1, refs.size)
        assertEquals("1111111111111111111111111111111111111111", refs["refs/tags/v1"])
    }

    @Test
    fun `empty file yields no refs`() {
        assertTrue(parser.parse("").isEmpty())
        assertNull(parser.parse("\n\n").get("refs/heads/main"))
    }
}
