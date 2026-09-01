package com.marfolog.noteswidgetformarkdown.presentation.widget

import com.marfolog.noteswidgetformarkdown.domain.model.GitSyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Turns [GitSyncStatus] into the short text + severity shown in the widget's bottom bar.
 * Pure logic, so it can be unit tested without a device clock.
 */
object GitSyncLabel {

    enum class Severity { Ok, Stale, Problem, Off }

    data class Label(val text: String, val severity: Severity)

    /**
     * Clients only touch the reflog when something actually transfers, so a quiet vault is
     * normal, not broken. Three days is long enough that silence really is suspicious.
     */
    private val STALE_AFTER_MILLIS = TimeUnit.HOURS.toMillis(72)

    fun format(status: GitSyncStatus, nowMillis: Long): Label = when (status) {
        is GitSyncStatus.NotTracked -> Label("no git", Severity.Off)
        is GitSyncStatus.Unavailable -> Label("git ?", Severity.Off)
        is GitSyncStatus.Tracked -> {
            // The text answers "what arrived and when"; the colour answers "is sync alive",
            // which includes the client merely checking with nothing to transfer.
            val at = maxOf(status.lastChangeAtMillis, status.lastFetchAtMillis ?: 0L)
            val transferAge = nowMillis - at
            val healthAge = nowMillis - maxOf(at, status.lastClientActivityAtMillis ?: 0L)
            // A stuck merge or rebase outranks freshness: the sync is broken, not just old.
            when (status.problem) {
                GitSyncStatus.Problem.Conflict -> Label("conflict", Severity.Problem)
                GitSyncStatus.Problem.RebaseInProgress -> Label("rebase", Severity.Problem)
                GitSyncStatus.Problem.Diverged -> Label("unpushed", Severity.Problem)
                GitSyncStatus.Problem.None -> {
                    // Name the operation, otherwise a bare "14:03" says nothing about what
                    // happened. The reflog action only describes the reflog entry — if the
                    // newer timestamp came from FETCH_HEAD, say "fetch" instead of mislabelling it.
                    val fetchIsNewer = (status.lastFetchAtMillis ?: 0L) > status.lastChangeAtMillis
                    val action = if (fetchIsNewer) "fetch" else status.lastAction ?: "change"
                    val text = "$action ${formatAge(at, transferAge)}"
                    if (healthAge in 0..STALE_AFTER_MILLIS) {
                        Label(text, Severity.Ok)
                    } else {
                        Label(text, Severity.Stale)
                    }
                }
            }
        }
    }

    private fun formatAge(atMillis: Long, ageMillis: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(ageMillis)
        return when {
            ageMillis < 0 -> TIME_FORMAT.format(Date(atMillis))
            days == 0L -> TIME_FORMAT.format(Date(atMillis))
            days == 1L -> "1 d"
            days < 100L -> "$days d"
            else -> "99+ d"
        }
    }

    private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())
}
