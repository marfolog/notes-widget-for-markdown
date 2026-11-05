package com.marfolog.noteswidgetformarkdown.data.parser

import com.marfolog.noteswidgetformarkdown.data.parser.GitBranchRelation.Ahead
import com.marfolog.noteswidgetformarkdown.data.parser.GitBranchRelation.Behind
import com.marfolog.noteswidgetformarkdown.data.parser.GitBranchRelation.Diverged
import com.marfolog.noteswidgetformarkdown.data.parser.GitBranchRelation.InSync
import com.marfolog.noteswidgetformarkdown.data.parser.GitReflogParser.Line
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class GitBranchRelationTest {

    private val prague = ZoneId.of("Europe/Prague")

    private fun at(day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, prague).toInstant().toEpochMilli()

    private val origin = "dae225cb0b0c4e0e8a1d6f7b5c3a29e41f8d7b60"
    private val first = "5a1c0f93e2b84d76a9f0c3d1e8b7a6254c9d0e13"
    private val second = "b7e4d2a1c9f3086e5d4a7b1c2e3f90a8d6c5b4e7"
    private val stranger = "0123456789abcdef0123456789abcdef01234567"

    private fun commit(old: String, new: String, day: Int, hour: Int, minute: Int, action: String = "commit") =
        Line(old, new, at(day, hour, minute), action)

    private val fetchedOrigin = listOf(
        Line(stranger, origin, at(2, 15, 2), "fetch")
    )

    @Test
    fun `one note committed on top of origin is unpushed since that commit`() {
        val branchLog = listOf(commit(origin, first, 5, 14, 2))

        assertEquals(Ahead(at(5, 14, 2)), GitBranchRelation.classify(first, origin, branchLog, fetchedOrigin))
    }

    @Test
    fun `several unpushed notes are unpushed since the first of them`() {
        val branchLog = listOf(
            commit(stranger, origin, 2, 15, 2, "pull"),
            commit(origin, first, 5, 9, 15),
            commit(first, second, 5, 14, 2)
        )

        assertEquals(Ahead(at(5, 9, 15)), GitBranchRelation.classify(second, origin, branchLog, fetchedOrigin))
    }

    @Test
    fun `a local branch on the remote tip is in sync`() {
        assertEquals(InSync, GitBranchRelation.classify(origin, origin, emptyList(), emptyList()))
    }

    @Test
    fun `shas are compared regardless of letter case`() {
        assertEquals(InSync, GitBranchRelation.classify(origin.uppercase(), origin, emptyList(), emptyList()))
        val branchLog = listOf(commit(origin.uppercase(), first.uppercase(), 5, 14, 2))
        assertEquals(Ahead(at(5, 14, 2)), GitBranchRelation.classify(first, origin, branchLog, emptyList()))
    }

    @Test
    fun `fetched but not merged is behind, not a problem`() {
        val remoteLog = listOf(
            Line(stranger, first, at(4, 8, 0), "fetch"),
            Line(first, origin, at(5, 10, 0), "fetch")
        )
        val branchLog = listOf(commit(stranger, first, 4, 8, 0, "pull"))

        assertEquals(Behind, GitBranchRelation.classify(first, origin, branchLog, remoteLog))
    }

    @Test
    fun `an amended commit is a divergence because history was rewritten`() {
        val branchLog = listOf(commit(origin, first, 5, 14, 2, "commit (amend)"))

        assertEquals(Diverged, GitBranchRelation.classify(first, origin, branchLog, fetchedOrigin))
    }

    @Test
    fun `a merge commit on top of origin is a divergence`() {
        val branchLog = listOf(commit(origin, first, 5, 14, 2, "commit (merge)"))

        assertEquals(Diverged, GitBranchRelation.classify(first, origin, branchLog, fetchedOrigin))
    }

    @Test
    fun `a reset in the middle of the chain is a divergence`() {
        val branchLog = listOf(
            commit(origin, first, 5, 9, 15),
            commit(first, second, 5, 14, 2, "reset")
        )

        assertEquals(Diverged, GitBranchRelation.classify(second, origin, branchLog, fetchedOrigin))
    }

    @Test
    fun `a gap in the chain is a divergence`() {
        val branchLog = listOf(
            commit(origin, first, 5, 9, 15),
            commit(stranger, second, 5, 14, 2)
        )

        assertEquals(Diverged, GitBranchRelation.classify(second, origin, branchLog, fetchedOrigin))
    }

    @Test
    fun `a chain that never reaches the remote tip is a divergence`() {
        val branchLog = listOf(commit(stranger, first, 5, 14, 2))

        assertEquals(Diverged, GitBranchRelation.classify(first, origin, branchLog, fetchedOrigin))
    }

    @Test
    fun `a reflog whose newest entry is not the local tip is a divergence`() {
        val branchLog = listOf(commit(origin, first, 5, 14, 2))

        assertEquals(Diverged, GitBranchRelation.classify(second, origin, branchLog, fetchedOrigin))
    }

    @Test
    fun `no reflog at all is a divergence`() {
        assertEquals(Diverged, GitBranchRelation.classify(first, origin, emptyList(), emptyList()))
    }

    @Test
    fun `a checkout on top of origin is not counted as unpushed work`() {
        val headLog = listOf(commit(origin, first, 5, 14, 2, "checkout"))

        assertEquals(Diverged, GitBranchRelation.classify(first, origin, headLog, fetchedOrigin))
    }

    @Test
    fun `unpushed commits win over a remote reflog that also mentions the local tip`() {
        val branchLog = listOf(commit(origin, first, 5, 14, 2))
        val remoteLog = listOf(Line(origin, first, at(5, 14, 5), "update by push"))

        assertEquals(Ahead(at(5, 14, 2)), GitBranchRelation.classify(first, origin, branchLog, remoteLog))
    }

    @Test
    fun `a phone whose login expired and then committed a note is unpushed since that note`() {
        val branchLog = GitReflogParser().parseLines(
            "7120de664c05c1c1f1fae489b745c63617e8878c dae225cbf5aa03cdd347b2ef62a99542fa8b3836 " +
                "Me <me@example.com> 1793628138 +0100\tFast-Forward: Setting refs/heads/main to id: " +
                "dae225cbf5aa03cdd347b2ef62a99542fa8b3836\n" +
                "dae225cbf5aa03cdd347b2ef62a99542fa8b3836 2daa70e5c4aebde36313348f0151d2217fe0c7c6 " +
                "Me <me@example.com> 1793883773 +0100\tcommit: Last Sync: 2026-11-05 14:02 (Mobile)\n"
        )
        val remoteLog = GitReflogParser().parseLines(
            "7120de664c05c1c1f1fae489b745c63617e8878c dae225cbf5aa03cdd347b2ef62a99542fa8b3836 " +
                "Me <me@example.com> 1793628134 +0100\tfetch origin\n"
        )

        val relation = GitBranchRelation.classify(
            localSha = "2daa70e5c4aebde36313348f0151d2217fe0c7c6",
            remoteSha = "dae225cbf5aa03cdd347b2ef62a99542fa8b3836",
            branchLog = branchLog,
            remoteLog = remoteLog
        )

        assertEquals(Ahead(1793883773000L), relation)
    }

    @Test
    fun `a corrupted local ref is no evidence of unpushed work`() {
        assertEquals(null, GitBranchRelation.classify("garbage!!", origin, emptyList(), fetchedOrigin))
    }

    @Test
    fun `a missing or empty remote ref is no evidence of unpushed work`() {
        assertEquals(null, GitBranchRelation.classify(first, null, emptyList(), fetchedOrigin))
        assertEquals(null, GitBranchRelation.classify(first, "", emptyList(), fetchedOrigin))
    }

    @Test
    fun `a truncated sha is not mistaken for a different commit`() {
        assertEquals(null, GitBranchRelation.classify(first.take(39), origin, emptyList(), fetchedOrigin))
    }

    @Test
    fun `sha-256 repositories are understood`() {
        val sha256 = "a".repeat(64)
        assertEquals(InSync, GitBranchRelation.classify(sha256, sha256.uppercase(), emptyList(), emptyList()))
    }
}
