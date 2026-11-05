package com.marfolog.noteswidgetformarkdown.presentation.widget

import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.domain.model.GitSyncStatus
import com.marfolog.noteswidgetformarkdown.presentation.widget.GitSyncLabel.Age
import com.marfolog.noteswidgetformarkdown.presentation.widget.GitSyncLabel.Severity
import com.marfolog.noteswidgetformarkdown.presentation.widget.GitSyncLabel.Trouble
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GitSyncLabelTest {

    private val prague = ZoneId.of("Europe/Prague")

    private fun at(day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, prague).toInstant().toEpochMilli()

    private val now = at(5, 14, 30)
    private val lastFetch = at(2, 15, 2)
    private val localCommit = at(5, 14, 2)
    private val hour = TimeUnit.HOURS.toMillis(1)
    private val minute = TimeUnit.MINUTES.toMillis(1)

    private fun hoursAgo(hours: Long) = now - hours * hour

    @Test
    fun `fresh pull is reported as ok`() {
        val label = GitSyncLabel.format(tracked(now - TimeUnit.HOURS.toMillis(2)), now)

        assertEquals(Severity.Ok, label.severity)
        assertNull(label.trouble)
        assertNull(label.detail)
    }

    @Test
    fun `a few quiet hours are still healthy`() {
        assertEquals(Severity.Ok, GitSyncLabel.format(tracked(now - TimeUnit.HOURS.toMillis(6)), now).severity)
    }

    @Test
    fun `the threshold from settings decides, not a hardcoded one`() {
        val twoDaysAgo = tracked(now - TimeUnit.DAYS.toMillis(2))

        assertEquals(Severity.Stale, GitSyncLabel.format(twoDaysAgo, now, staleAfterHours = 24).severity)
        assertEquals(Severity.Ok, GitSyncLabel.format(twoDaysAgo, now, staleAfterHours = 168).severity)
    }

    @Test
    fun `silence beyond the default day is stale and counted in days`() {
        val label = GitSyncLabel.format(tracked(now - TimeUnit.DAYS.toMillis(2)), now)

        assertEquals(Severity.Stale, label.severity)
        assertEquals(R.string.sync_synced, label.textRes)
        assertEquals(Age.Days(2), label.arg)
        assertEquals(Trouble.Stale, label.trouble)
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

        assertEquals(Severity.Ok, label.severity)
        assertEquals(R.string.sync_synced, label.textRes)
    }

    @Test
    fun `untracked vault is reported as off`() {
        val label = GitSyncLabel.format(GitSyncStatus.NotTracked, now)

        assertEquals(Severity.Off, label.severity)
        assertEquals(R.string.sync_no_info, label.textRes)
    }

    @Test
    fun `unreadable git state is reported as off, not as a problem`() {
        val label = GitSyncLabel.format(GitSyncStatus.Unavailable("no permission"), now)

        assertEquals(Severity.Off, label.severity)
        assertEquals(R.string.sync_unknown, label.textRes)
    }

    @Test
    fun `age caps at 99 plus days`() {
        val label = GitSyncLabel.format(tracked(now - TimeUnit.DAYS.toMillis(365)), now)

        assertEquals(Age.Days(365), label.arg)
    }

    // region unfinished operations

    @Test
    fun `an unfinished merge beats freshness and says sync stuck`() {
        val status = tracked(now - TimeUnit.MINUTES.toMillis(5)).copy(problem = GitSyncStatus.Problem.Conflict)

        val label = GitSyncLabel.format(status, now)

        assertEquals(Severity.Problem, label.severity)
        assertEquals(R.string.sync_stuck, label.textRes)
        assertEquals(Trouble.Stuck, label.trouble)
        assertEquals(R.string.git_problem_conflict, label.detail?.textRes)
    }

    @Test
    fun `an interrupted rebase says sync stuck with its own explanation`() {
        val status = tracked(now).copy(problem = GitSyncStatus.Problem.RebaseInProgress)

        val label = GitSyncLabel.format(status, now)

        assertEquals(R.string.sync_stuck, label.textRes)
        assertEquals(R.string.git_problem_rebase, label.detail?.textRes)
    }

    @Test
    fun `a real divergence is reported as not pushed whatever the freshness`() {
        val status = tracked(now).copy(problem = GitSyncStatus.Problem.Diverged)

        val label = GitSyncLabel.format(status, now)

        assertEquals(Severity.Problem, label.severity)
        assertEquals(R.string.sync_not_pushed, label.textRes)
        assertEquals(Trouble.NotPushed, label.trouble)
        assertEquals(R.string.git_problem_diverged, label.detail?.textRes)
    }

    // endregion

    // region the incident: expired login, a note committed locally

    @Test
    fun `a phone with an expired login that commits a note says can't connect`() {
        val label = GitSyncLabel.format(incident(), now)

        assertEquals(Severity.Problem, label.severity)
        assertEquals(R.string.sync_cant_connect, label.textRes)
        assertEquals(Trouble.CantConnect, label.trouble)
        assertEquals(R.string.git_problem_cant_connect, label.detail?.textRes)
        assertEquals(lastFetch, label.detail?.atMillis)
    }

    @Test
    fun `a note committed a minute ago is not reported as unpushed`() {
        val status = incident(unpushedSince = now - minute)

        val label = GitSyncLabel.format(status, now)

        assertEquals(Severity.Stale, label.severity)
        assertEquals(R.string.sync_synced, label.textRes)
    }

    @Test
    fun `a note committed exactly fifteen minutes ago is still given time`() {
        val label = GitSyncLabel.format(incident(unpushedSince = now - 15 * minute), now)

        assertEquals(R.string.sync_synced, label.textRes)
    }

    @Test
    fun `a note committed just over fifteen minutes ago is reported`() {
        val label = GitSyncLabel.format(incident(unpushedSince = now - 15 * minute - 1), now)

        assertEquals(R.string.sync_cant_connect, label.textRes)
    }

    @Test
    fun `a fresh note on a phone that fetched an hour ago stays green`() {
        val status = incident(unpushedSince = now - minute, fetchAt = now - hour)

        assertEquals(Severity.Ok, GitSyncLabel.format(status, now).severity)
    }

    @Test
    fun `a push refused after a successful fetch says not pushed`() {
        val status = incident(fetchAt = localCommit + 5 * minute)

        val label = GitSyncLabel.format(status, now)

        assertEquals(Severity.Problem, label.severity)
        assertEquals(R.string.sync_not_pushed, label.textRes)
        assertEquals(Trouble.NotPushed, label.trouble)
        assertEquals(R.string.git_problem_not_pushed, label.detail?.textRes)
    }

    @Test
    fun `contact in the very minute of the commit counts as the push being refused`() {
        val label = GitSyncLabel.format(incident(fetchAt = localCommit), now)

        assertEquals(R.string.sync_not_pushed, label.textRes)
    }

    @Test
    fun `contact one millisecond before the commit does not explain the unpushed note`() {
        val label = GitSyncLabel.format(incident(fetchAt = localCommit - 1), now)

        assertEquals(R.string.sync_cant_connect, label.textRes)
    }

    @Test
    fun `a push that moved the remote ref counts as contact with the server`() {
        val status = incident(remoteRefAt = localCommit + minute)

        assertEquals(R.string.sync_not_pushed, GitSyncLabel.format(status, now).textRes)
    }

    @Test
    fun `a pull after the commit counts as contact with the server`() {
        val status = incident(pullAt = localCommit + minute)

        assertEquals(R.string.sync_not_pushed, GitSyncLabel.format(status, now).textRes)
    }

    @Test
    fun `unpushed work from a client that leaves no remote trace says not pushed, not can't connect`() {
        val status = incident(fetchAt = null)

        val label = GitSyncLabel.format(status, now)

        assertEquals(R.string.sync_not_pushed, label.textRes)
        assertEquals(GitSyncLabel.Severity.Problem, label.severity)
    }

    @Test
    fun `a clock that runs behind never reports a fresh commit as unpushed`() {
        val status = incident(unpushedSince = now + hour)

        assertEquals(Severity.Stale, GitSyncLabel.format(status, now).severity)
    }

    // endregion

    // region freshness comes from the remote only

    @Test
    fun `a local commit does not turn an old chip green`() {
        val status = tracked(localCommit).copy(lastFetchAtMillis = lastFetch)

        val label = GitSyncLabel.format(status, now)

        assertEquals(Severity.Stale, label.severity)
        assertEquals(Age.Days(2), label.arg)
    }

    @Test
    fun `a client that only checks the index stays amber when it never reached the remote`() {
        val status = tracked(localCommit)
            .copy(lastFetchAtMillis = lastFetch, lastClientActivityAtMillis = now - minute)

        assertEquals(Severity.Stale, GitSyncLabel.format(status, now).severity)
    }

    @Test
    fun `a client that writes nothing remote related keeps being judged by its index`() {
        val status = tracked(now - TimeUnit.DAYS.toMillis(3))
            .copy(lastClientActivityAtMillis = now - TimeUnit.HOURS.toMillis(1))

        val label = GitSyncLabel.format(status, now)

        assertEquals(Severity.Ok, label.severity)
        assertEquals(Age.Days(3), label.arg)
    }

    @Test
    fun `a push alone keeps the chip green`() {
        val status = tracked(now - TimeUnit.DAYS.toMillis(3))
            .copy(lastFetchAtMillis = lastFetch, lastRemoteRefUpdateAtMillis = now - 10 * minute)

        val label = GitSyncLabel.format(status, now)

        assertEquals(Severity.Ok, label.severity)
        assertEquals(Age.Clock(clock(now - 10 * minute)), label.arg)
    }

    @Test
    fun `a pull from the reflog alone keeps the chip green`() {
        val status = tracked(now).copy(lastPullAtMillis = now - 2 * hour)

        assertEquals(Severity.Ok, GitSyncLabel.format(status, now).severity)
    }

    @Test
    fun `the newest of the three remote signals decides`() {
        val status = tracked(now).copy(
            lastFetchAtMillis = hoursAgo(80),
            lastRemoteRefUpdateAtMillis = hoursAgo(50),
            lastPullAtMillis = hoursAgo(2)
        )

        assertEquals(Severity.Ok, GitSyncLabel.format(status, now).severity)
        assertEquals(hoursAgo(2), status.lastRemoteContactAtMillis)
    }

    @Test
    fun `fetched but not merged is not a problem`() {
        val status = tracked(now).copy(
            problem = GitSyncStatus.Problem.None,
            lastFetchAtMillis = now - 10 * minute
        )

        assertEquals(Severity.Ok, GitSyncLabel.format(status, now).severity)
    }

    // endregion

    // region silence thresholds

    @Test
    fun `contact right at the threshold is still green`() {
        assertEquals(Severity.Ok, GitSyncLabel.format(fetched(hoursAgo(24)), now, 24).severity)
    }

    @Test
    fun `contact just past the threshold turns amber`() {
        assertEquals(Severity.Stale, GitSyncLabel.format(fetched(hoursAgo(24) - 1), now, 24).severity)
    }

    @Test
    fun `two quiet days with a daily threshold are amber and explain how to raise it`() {
        val label = GitSyncLabel.format(fetched(now - TimeUnit.DAYS.toMillis(2)), now, 24)

        assertEquals(Severity.Stale, label.severity)
        assertEquals(R.string.git_problem_stale, label.detail?.textRes)
        assertEquals(Age.Days(2), label.detail?.duration)
    }

    @Test
    fun `silence right at three thresholds stays amber`() {
        assertEquals(Severity.Stale, GitSyncLabel.format(fetched(hoursAgo(72)), now, 24).severity)
    }

    @Test
    fun `three quiet days of a client that does fetch turn red`() {
        val label = GitSyncLabel.format(fetched(hoursAgo(72) - 1), now, 24)

        assertEquals(Severity.Problem, label.severity)
        assertEquals(R.string.sync_cant_connect, label.textRes)
        assertEquals(Trouble.CantConnect, label.trouble)
        assertEquals(R.string.git_problem_silent_long, label.detail?.textRes)
        assertEquals(Age.Days(3), label.detail?.duration)
    }

    @Test
    fun `the incident silence alone, a little under three days, is still amber with a daily threshold`() {
        val label = GitSyncLabel.format(fetched(lastFetch), now, 24)

        assertEquals(Severity.Stale, label.severity)
    }

    @Test
    fun `a weekly threshold is amber for a week and red only after three`() {
        val week = 168
        assertEquals(Severity.Ok, GitSyncLabel.format(fetched(hoursAgo(168)), now, week).severity)
        assertEquals(Severity.Stale, GitSyncLabel.format(fetched(hoursAgo(400)), now, week).severity)
        assertEquals(Severity.Stale, GitSyncLabel.format(fetched(hoursAgo(504)), now, week).severity)
        assertEquals(Severity.Problem, GitSyncLabel.format(fetched(hoursAgo(505)), now, week).severity)
    }

    @Test
    fun `red never comes before amber for any threshold`() {
        listOf(6, 24, 72, 168).forEach { threshold ->
            val justPast = now - TimeUnit.HOURS.toMillis(threshold.toLong()) - 1
            assertEquals(Severity.Stale, GitSyncLabel.format(fetched(justPast), now, threshold).severity)
        }
    }

    @Test
    fun `a client without FETCH_HEAD stays amber after a quiet week`() {
        val status = tracked(now - TimeUnit.DAYS.toMillis(7))
            .copy(lastRemoteRefUpdateAtMillis = now - TimeUnit.DAYS.toMillis(7))

        val label = GitSyncLabel.format(status, now, 24)

        assertEquals(Severity.Stale, label.severity)
        assertEquals(R.string.sync_synced, label.textRes)
    }

    @Test
    fun `a client without any remote trace stays amber after a quiet week`() {
        val label = GitSyncLabel.format(tracked(now - TimeUnit.DAYS.toMillis(7)), now, 24)

        assertEquals(Severity.Stale, label.severity)
    }

    @Test
    fun `short silence is counted in hours in the explanation`() {
        val label = GitSyncLabel.format(fetched(hoursAgo(10)), now, 6)

        assertEquals(Age.Hours(10), label.detail?.duration)
    }

    @Test
    fun `a device clock that runs behind never raises an alarm`() {
        val label = GitSyncLabel.format(fetched(now + TimeUnit.DAYS.toMillis(10)), now, 6)

        assertEquals(Severity.Ok, label.severity)
        assertNull(label.trouble)
    }

    @Test
    fun `a device clock that runs behind shows the time instead of a negative age`() {
        val future = now + hour

        val label = GitSyncLabel.format(fetched(future), now, 24)

        assertEquals(Age.Clock(clock(future)), label.arg)
    }

    // endregion

    // region file activity

    @Test
    fun `recent file activity without git is reported as updated and ok`() {
        val label = GitSyncLabel.format(GitSyncStatus.FileActivity(now - TimeUnit.HOURS.toMillis(2)), now)

        assertEquals(Severity.Ok, label.severity)
        assertEquals(R.string.sync_updated, label.textRes)
    }

    @Test
    fun `file activity honours the threshold from settings`() {
        val status = GitSyncStatus.FileActivity(now - TimeUnit.DAYS.toMillis(2))

        assertEquals(Severity.Stale, GitSyncLabel.format(status, now, staleAfterHours = 24).severity)
        assertEquals(Severity.Ok, GitSyncLabel.format(status, now, staleAfterHours = 72).severity)
    }

    // endregion

    @Test
    fun `every trouble is counted under its own analytics type`() {
        assertEquals(
            listOf("cant_connect", "not_pushed", "stuck", "stale"),
            Trouble.values().map { it.analyticsType }
        )
    }

    @Test
    fun `the incident warns in settings as a problem with the reconnect advice`() {
        val warning = GitSyncLabel.warningOf(GitSyncLabel.format(incident(), now))

        assertEquals(true, warning?.isProblem)
        assertEquals(R.string.git_problem_cant_connect, warning?.detail?.textRes)
    }

    @Test
    fun `two quiet days warn in settings, but only as amber`() {
        val warning = GitSyncLabel.warningOf(GitSyncLabel.format(fetched(hoursAgo(48)), now))

        assertEquals(false, warning?.isProblem)
        assertEquals(R.string.git_problem_stale, warning?.detail?.textRes)
    }

    @Test
    fun `a healthy sync shows no warning in settings`() {
        assertEquals(null, GitSyncLabel.warningOf(GitSyncLabel.format(fetched(hoursAgo(1)), now)))
    }

    private fun clock(millis: Long): String =
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(millis))

    private fun fetched(fetchAt: Long) = tracked(fetchAt).copy(lastFetchAtMillis = fetchAt)

    private fun tracked(atMillis: Long) = GitSyncStatus.Tracked(
        lastChangeAtMillis = atMillis,
        lastAction = "pull",
        lastFetchAtMillis = null,
        branch = "main"
    )

    private fun incident(
        unpushedSince: Long = localCommit,
        fetchAt: Long? = lastFetch,
        remoteRefAt: Long? = null,
        pullAt: Long? = null
    ) = GitSyncStatus.Tracked(
        lastChangeAtMillis = localCommit,
        lastAction = "commit",
        lastFetchAtMillis = fetchAt,
        branch = "main",
        lastClientActivityAtMillis = now - minute,
        lastRemoteRefUpdateAtMillis = remoteRefAt,
        lastPullAtMillis = pullAt,
        unpushedSinceMillis = unpushedSince,
        problem = GitSyncStatus.Problem.Ahead
    )
}
