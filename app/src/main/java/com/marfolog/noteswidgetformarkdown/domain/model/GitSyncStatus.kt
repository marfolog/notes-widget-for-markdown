package com.marfolog.noteswidgetformarkdown.domain.model

/**
 * Health of the git sync behind the vault folder.
 *
 * The app never runs git itself — it only reads the metadata a git client
 * (obsidian-git, GitSync, Termux…) leaves behind in `.git/`.
 */
sealed interface GitSyncStatus {

    /** No `.git` folder under the vault root, or it is not readable through SAF. */
    data object NotTracked : GitSyncStatus

    /** `.git` exists but the interesting files could not be read. */
    data class Unavailable(val reason: String) : GitSyncStatus

    /**
     * Folder is synced by something other than git — Syncthing, a cloud drive, a paid sync
     * service. None of them leave readable state behind, so the only honest signal is when a
     * note last changed.
     *
     * @param lastChangeAtMillis mtime of the newest note in the folder
     */
    data class FileActivity(val lastChangeAtMillis: Long) : GitSyncStatus

    /**
     * Freshness is judged by contact with the remote only — a local commit or a client poking
     * `.git/index` proves nothing reached the server.
     *
     * @param lastChangeAtMillis when HEAD last moved (commit, pull, checkout…)
     * @param lastAction reflog action, e.g. `pull` or `commit`
     * @param lastFetchAtMillis mtime of `FETCH_HEAD`: rewritten by every successful fetch, even an
     *   empty one, and left alone when authentication fails. Null means the client never writes it.
     * @param branch current branch name, if resolvable
     * @param lastClientActivityAtMillis mtime of `.git/index`. Not a sign of contact with the
     *   remote; only a fallback health signal for clients that write nothing remote-related at all
     * @param lastRemoteRefUpdateAtMillis last entry of the remote-tracking branch's reflog, which a
     *   push moves as well
     * @param lastPullAtMillis last pull, fast-forward or clone in the HEAD reflog
     * @param unpushedSinceMillis when the oldest commit not yet on the remote was made, set with
     *   [Problem.Ahead]
     * @param problem an unfinished merge/rebase, unpushed commits or a branch that drifted
     */
    data class Tracked(
        val lastChangeAtMillis: Long,
        val lastAction: String?,
        val lastFetchAtMillis: Long?,
        val branch: String?,
        val lastClientActivityAtMillis: Long? = null,
        val lastRemoteRefUpdateAtMillis: Long? = null,
        val lastPullAtMillis: Long? = null,
        val unpushedSinceMillis: Long? = null,
        val problem: Problem = Problem.None
    ) : GitSyncStatus {

        /** Newest sign that the client talked to the remote, null when none was ever left behind. */
        val lastRemoteContactAtMillis: Long?
            get() {
                val fetch = lastFetchAtMillis ?: Long.MIN_VALUE
                val ref = lastRemoteRefUpdateAtMillis ?: Long.MIN_VALUE
                val pull = lastPullAtMillis ?: Long.MIN_VALUE
                return maxOf(fetch, ref, pull).takeIf { it != Long.MIN_VALUE }
            }
    }

    /** Trouble the sync client left behind, detectable from `.git` alone. */
    enum class Problem {
        /** Nothing unusual: no half-finished operation, local matches or is behind the remote. */
        None,

        /** `MERGE_HEAD` present — a merge stopped, in practice almost always a conflict. */
        Conflict,

        /** `rebase-merge` / `rebase-apply` present — a rebase was interrupted. */
        RebaseInProgress,

        /** Plain local commits sit on top of the remote tip; see `unpushedSinceMillis`. */
        Ahead,

        /** Local branch and the remote-tracking ref point at different, unrelated-looking commits. */
        Diverged
    }
}
