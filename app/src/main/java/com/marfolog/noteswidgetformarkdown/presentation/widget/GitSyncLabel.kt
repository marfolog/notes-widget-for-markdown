package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import androidx.annotation.StringRes
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.domain.model.GitSyncStatus
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Turns [GitSyncStatus] into the short text + severity shown in the widget's bottom bar.
 *
 * The chip answers "are my notes up to date", not "what did git do" — git verbs like *commit* or
 * *rebase* mean nothing to someone syncing another way, and the same chip will later cover those
 * setups. Which git operation it actually was belongs in settings, where there is room to say it.
 *
 * Pure logic, so it can be unit tested without a device clock.
 */
object GitSyncLabel {

    enum class Severity { Ok, Stale, Problem, Off }

    /** What went wrong, as [UsageEvent.SYNC_PROBLEM_SHOWN] counts it. */
    enum class Trouble(val analyticsType: String) {
        CantConnect(UsageEvent.SYNC_PROBLEM_CANT_CONNECT),
        NotPushed(UsageEvent.SYNC_PROBLEM_NOT_PUSHED),
        Stuck(UsageEvent.SYNC_PROBLEM_STUCK),
        Stale(UsageEvent.SYNC_PROBLEM_STALE)
    }

    /**
     * A time or a span kept as numbers: the words for it ("2 d", "2 Tage") come from translated
     * resources, which a pure function cannot reach.
     */
    sealed interface Age {
        data class Clock(val text: String) : Age
        data class Hours(val hours: Long) : Age
        data class Days(val days: Long) : Age
    }

    /** Plain-language explanation for settings. [atMillis] and [duration] fill the text's `%1$s`. */
    data class Detail(
        @StringRes val textRes: Int,
        val atMillis: Long? = null,
        val duration: Age? = null
    )

    /**
     * Text se nese jako ID retezce a pripadny argument, ne jako hotova veta. Logika tim zustava
     * bez Contextu a testovatelna na JVM, prekladat se da presto.
     */
    data class Label(
        @StringRes val textRes: Int,
        val severity: Severity,
        val arg: Age? = null,
        val trouble: Trouble? = null,
        val detail: Detail? = null
    )

    /**
     * Den ticha uz stoji za pozornost: bezny klient se hlasi po minutach az hodinach, takze
     * cely den bez jedineho prenosu i bez jedine zmeny poznamky je podezrely. Uzivatel si prah
     * muze prehodit v nastaveni.
     */
    val DEFAULT_STALE_HOURS = 24

    /** A commit made a moment ago is just waiting for the client's next run. */
    val UNPUSHED_GRACE_MILLIS = TimeUnit.MINUTES.toMillis(15)

    /** Silence up to this many thresholds is amber; beyond it the client is considered cut off. */
    const val RED_AFTER_THRESHOLDS = 3

    fun format(
        status: GitSyncStatus,
        nowMillis: Long,
        staleAfterHours: Int = DEFAULT_STALE_HOURS
    ): Label {
        val staleAfterMillis = TimeUnit.HOURS.toMillis(staleAfterHours.toLong())
        return when (status) {
            is GitSyncStatus.NotTracked -> Label(R.string.sync_no_info, Severity.Off)
            is GitSyncStatus.Unavailable -> Label(R.string.sync_unknown, Severity.Off)
            is GitSyncStatus.FileActivity -> fileActivity(status, nowMillis, staleAfterMillis)
            is GitSyncStatus.Tracked -> tracked(status, nowMillis, staleAfterMillis)
        }
    }

    private fun fileActivity(
        status: GitSyncStatus.FileActivity,
        nowMillis: Long,
        staleAfterMillis: Long
    ): Label {
        // Bez gitu nevime, jestli sync bezi — vime jen, kdy se naposled neco zmenilo.
        // Ticho delsi nez prah je jediny signal, ktery muzeme nabidnout.
        val age = nowMillis - status.lastChangeAtMillis
        val at = formatAge(status.lastChangeAtMillis, age)
        return if (age in 0..staleAfterMillis) {
            Label(R.string.sync_updated, Severity.Ok, at)
        } else {
            Label(R.string.sync_updated, Severity.Stale, at)
        }
    }

    private fun tracked(
        status: GitSyncStatus.Tracked,
        nowMillis: Long,
        staleAfterMillis: Long
    ): Label = when (status.problem) {
        GitSyncStatus.Problem.Conflict -> stuck(R.string.git_problem_conflict)
        GitSyncStatus.Problem.RebaseInProgress -> stuck(R.string.git_problem_rebase)
        GitSyncStatus.Problem.Diverged -> Label(
            R.string.sync_not_pushed,
            Severity.Problem,
            trouble = Trouble.NotPushed,
            detail = Detail(R.string.git_problem_diverged)
        )
        GitSyncStatus.Problem.Ahead -> unpushed(status, nowMillis)
            ?: freshness(status, nowMillis, staleAfterMillis)
        GitSyncStatus.Problem.None -> freshness(status, nowMillis, staleAfterMillis)
    }

    private fun stuck(@StringRes detail: Int) = Label(
        R.string.sync_stuck,
        Severity.Problem,
        trouble = Trouble.Stuck,
        detail = Detail(detail)
    )

    /**
     * Commits older than the grace period that never reached the server. Whether the client
     * reached the server at all since the first of them tells a dead login from a refused push.
     */
    private fun unpushed(status: GitSyncStatus.Tracked, nowMillis: Long): Label? {
        val since = status.unpushedSinceMillis ?: return null
        if (nowMillis - since <= UNPUSHED_GRACE_MILLIS) {
            return null
        }
        // Bez jedine stopy po serveru nevime, jestli se klient pripojit nesnazil, nebo nemohl.
        // Chybejici data nejsou dukaz, takze jen "neodeslano", ne "nepripoji se".
        val contact = status.lastRemoteContactAtMillis
        return if (contact == null || contact >= since) {
            Label(
                R.string.sync_not_pushed,
                Severity.Problem,
                trouble = Trouble.NotPushed,
                detail = Detail(R.string.git_problem_not_pushed)
            )
        } else {
            Label(
                R.string.sync_cant_connect,
                Severity.Problem,
                trouble = Trouble.CantConnect,
                detail = Detail(R.string.git_problem_cant_connect, atMillis = contact)
            )
        }
    }

    /**
     * Only contact with the remote keeps the chip green. A client that leaves no remote trace at
     * all is judged as before, by its last HEAD move and `.git/index`, since nothing better exists.
     */
    private fun freshness(
        status: GitSyncStatus.Tracked,
        nowMillis: Long,
        staleAfterMillis: Long
    ): Label {
        val contact = status.lastRemoteContactAtMillis
        val shownAt = contact ?: status.lastChangeAtMillis
        val healthAt = contact ?: maxOf(status.lastChangeAtMillis, status.lastClientActivityAtMillis ?: 0L)
        val age = nowMillis - healthAt
        val shown = formatAge(shownAt, nowMillis - shownAt)
        val silence = spanOf(age)
        return when {
            age <= staleAfterMillis -> Label(R.string.sync_synced, Severity.Ok, shown)
            age > staleAfterMillis * RED_AFTER_THRESHOLDS && status.lastFetchAtMillis != null -> Label(
                R.string.sync_cant_connect,
                Severity.Problem,
                trouble = Trouble.CantConnect,
                detail = Detail(R.string.git_problem_silent_long, duration = silence)
            )
            else -> Label(
                R.string.sync_synced,
                Severity.Stale,
                shown,
                trouble = Trouble.Stale,
                detail = Detail(R.string.git_problem_stale, duration = silence)
            )
        }
    }

    private fun spanOf(ageMillis: Long): Age {
        val hours = TimeUnit.MILLISECONDS.toHours(ageMillis)
        return if (hours < HOURS_PER_DAY) Age.Hours(maxOf(hours, 1L)) else Age.Days(hours / HOURS_PER_DAY)
    }

    private fun formatAge(atMillis: Long, ageMillis: Long): Age {
        val days = TimeUnit.MILLISECONDS.toDays(ageMillis)
        return if (ageMillis < 0 || days == 0L) {
            Age.Clock(SimpleDateFormat(CLOCK_PATTERN, Locale.getDefault()).format(Date(atMillis)))
        } else {
            Age.Days(days)
        }
    }

    /** What settings should warn about; null when the chip is green or there is nothing to explain. */
    data class Warning(val detail: Detail, val isProblem: Boolean)

    fun warningOf(label: Label): Warning? {
        val detail = label.detail ?: return null
        return when (label.severity) {
            Severity.Problem -> Warning(detail, isProblem = true)
            Severity.Stale -> Warning(detail, isProblem = false)
            Severity.Ok, Severity.Off -> null
        }
    }

    private const val CLOCK_PATTERN = "HH:mm"
    private const val HOURS_PER_DAY = 24L
}

internal fun GitSyncLabel.Age.text(context: Context): String = when (this) {
    is GitSyncLabel.Age.Clock -> text
    is GitSyncLabel.Age.Hours -> context.getString(R.string.sync_hours, hours)
    is GitSyncLabel.Age.Days -> when {
        days == 1L -> context.getString(R.string.sync_days_one)
        days < DAYS_CAP -> context.getString(R.string.sync_days, days)
        else -> context.getString(R.string.sync_days_many)
    }
}

internal fun GitSyncLabel.Detail.text(context: Context): String = when {
    atMillis != null -> context.getString(
        textRes,
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(atMillis))
    )
    duration != null -> context.getString(textRes, duration.text(context))
    else -> context.getString(textRes)
}

private const val DAYS_CAP = 100L
