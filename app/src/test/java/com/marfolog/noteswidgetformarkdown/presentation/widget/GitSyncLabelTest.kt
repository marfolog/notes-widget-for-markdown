package com.marfolog.noteswidgetformarkdown.presentation.widget

import com.marfolog.noteswidgetformarkdown.R
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
    fun `a few quiet hours are still healthy`() {
        val label = GitSyncLabel.format(tracked(now - TimeUnit.HOURS.toMillis(6)), now)

        assertEquals(GitSyncLabel.Severity.Ok, label.severity)
    }

    @Test
    fun `the threshold from settings decides, not a hardcoded one`() {
        val twoDaysAgo = tracked(now - TimeUnit.DAYS.toMillis(2))

        assertEquals(
            GitSyncLabel.Severity.Stale,
            GitSyncLabel.format(twoDaysAgo, now, staleAfterHours = 24).severity
        )
        assertEquals(
            GitSyncLabel.Severity.Ok,
            GitSyncLabel.format(twoDaysAgo, now, staleAfterHours = 168).severity
        )
    }

    @Test
    fun `silence beyond the default day is stale`() {
        val label = GitSyncLabel.format(tracked(now - TimeUnit.DAYS.toMillis(4)), now)

        assertEquals(GitSyncLabel.Severity.Stale, label.severity)
        assertEquals(R.string.sync_synced, label.textRes)
        assertEquals("4 d", label.arg)
    }

    @Test
    fun `a newer fetch still counts as synced`() {
        val status = GitSyncStatus.Tracked(
            lastChangeAtMillis = now - TimeUnit.DAYS.toMillis(5),
            lastAction = "commit",
            lastFetchAtMillis = now - TimeUnit.MINUTES.toMillis(10),
            branch = "main"
        )

        val label = GitSyncLabel.format(status, now)

        assertEquals(GitSyncLabel.Severity.Ok, label.severity)
        assertEquals(R.string.sync_synced, label.textRes)
    }

    @Test
    fun `untracked vault is reported as off`() {
        val label = GitSyncLabel.format(GitSyncStatus.NotTracked, now)

        assertEquals(GitSyncLabel.Severity.Off, label.severity)
        assertEquals(R.string.sync_no_info, label.textRes)
    }

    @Test
    fun `conflict beats freshness`() {
        val status = tracked(now - TimeUnit.MINUTES.toMillis(5))
            .copy(problem = GitSyncStatus.Problem.Conflict)
        val label = GitSyncLabel.format(status, now)

        assertEquals(GitSyncLabel.Severity.Problem, label.severity)
        assertEquals(R.string.sync_stuck, label.textRes)
    }

    @Test
    fun `diverged branch is reported as unpushed`() {
        val status = tracked(now).copy(problem = GitSyncStatus.Problem.Diverged)

        assertEquals(R.string.sync_not_pushed, GitSyncLabel.format(status, now).textRes)
    }

    @Test
    fun `recent file activity without git is reported as updated and ok`() {
        val status = GitSyncStatus.FileActivity(now - TimeUnit.HOURS.toMillis(2))

        val label = GitSyncLabel.format(status, now)

        assertEquals(GitSyncLabel.Severity.Ok, label.severity)
        assertEquals(R.string.sync_updated, label.textRes)
    }

    @Test
    fun `file activity honours the threshold from settings`() {
        val status = GitSyncStatus.FileActivity(now - TimeUnit.DAYS.toMillis(2))

        assertEquals(
            GitSyncLabel.Severity.Stale,
            GitSyncLabel.format(status, now, staleAfterHours = 24).severity
        )
        assertEquals(
            GitSyncLabel.Severity.Ok,
            GitSyncLabel.format(status, now, staleAfterHours = 72).severity
        )
    }

    @Test
    fun `unreadable git state is reported as off, not as a problem`() {
        val label = GitSyncLabel.format(GitSyncStatus.Unavailable("no permission"), now)

        assertEquals(GitSyncLabel.Severity.Off, label.severity)
        assertEquals(R.string.sync_unknown, label.textRes)
    }

    @Test
    fun `recent client check keeps colour ok while text still shows the old transfer`() {
        // The client fetched with nothing to transfer: sync is alive (colour), but the last
        // actual change is days old (text). The two must not get mixed up.
        val status = tracked(now - TimeUnit.DAYS.toMillis(3))
            .copy(lastClientActivityAtMillis = now - TimeUnit.HOURS.toMillis(1))

        val label = GitSyncLabel.format(status, now)

        assertEquals(GitSyncLabel.Severity.Ok, label.severity)
        assertEquals("3 d", label.arg)
    }

    @Test
    fun `age caps at 99 plus days`() {
        val label = GitSyncLabel.format(tracked(now - TimeUnit.DAYS.toMillis(365)), now)

        assertEquals("99+ d", label.arg)
    }

    private fun tracked(atMillis: Long) = GitSyncStatus.Tracked(
        lastChangeAtMillis = atMillis,
        lastAction = "pull",
        lastFetchAtMillis = null,
        branch = "main"
    )
}
