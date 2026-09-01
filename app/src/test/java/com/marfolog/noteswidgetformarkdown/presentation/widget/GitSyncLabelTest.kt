package com.marfolog.noteswidgetformarkdown.presentation.widget

import com.marfolog.noteswidgetformarkdown.domain.model.GitSyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class GitSyncLabelTest {

    private val now = 1_735_862_400_000L

    @Test
    fun `fresh pull is reported as ok`() {
        val label = GitSyncLabel.format(tracked(now - TimeUnit.HOURS.toMillis(2)), now)

        assertEquals(GitSyncLabel.Severity.Ok, label.severity)
    }

    @Test
    fun `a quiet day is still healthy`() {
        val label = GitSyncLabel.format(tracked(now - TimeUnit.DAYS.toMillis(2)), now)

        assertEquals(GitSyncLabel.Severity.Ok, label.severity)
        assertEquals("pull 2 d", label.text)
    }

    @Test
    fun `silence beyond three days is stale`() {
        val label = GitSyncLabel.format(tracked(now - TimeUnit.DAYS.toMillis(4)), now)

        assertEquals(GitSyncLabel.Severity.Stale, label.severity)
        assertEquals("pull 4 d", label.text)
    }

    @Test
    fun `a newer fetch is labelled fetch, not the stale reflog action`() {
        val status = GitSyncStatus.Tracked(
            lastChangeAtMillis = now - TimeUnit.DAYS.toMillis(5),
            lastAction = "commit",
            lastFetchAtMillis = now - TimeUnit.MINUTES.toMillis(10),
            branch = "main"
        )

        val label = GitSyncLabel.format(status, now)

        assertEquals(GitSyncLabel.Severity.Ok, label.severity)
        assertTrue(label.text.startsWith("fetch "))
    }

    @Test
    fun `untracked vault is reported as off`() {
        val label = GitSyncLabel.format(GitSyncStatus.NotTracked, now)

        assertEquals(GitSyncLabel.Severity.Off, label.severity)
        assertEquals("no git", label.text)
    }

    @Test
    fun `conflict beats freshness`() {
        val status = tracked(now - TimeUnit.MINUTES.toMillis(5))
            .copy(problem = GitSyncStatus.Problem.Conflict)
        val label = GitSyncLabel.format(status, now)

        assertEquals(GitSyncLabel.Severity.Problem, label.severity)
        assertEquals("conflict", label.text)
    }

    @Test
    fun `diverged branch is reported as unpushed`() {
        val status = tracked(now).copy(problem = GitSyncStatus.Problem.Diverged)

        assertEquals("unpushed", GitSyncLabel.format(status, now).text)
    }

    private fun tracked(atMillis: Long) = GitSyncStatus.Tracked(
        lastChangeAtMillis = atMillis,
        lastAction = "pull",
        lastFetchAtMillis = null,
        branch = "main"
    )
}
