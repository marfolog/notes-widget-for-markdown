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
     * @param lastChangeAtMillis when HEAD last moved (commit, pull, checkout…)
     * @param lastAction reflog action, e.g. `pull` or `commit`
     * @param lastFetchAtMillis when the client last talked to the remote, if known
     * @param branch current branch name, if resolvable
     * @param lastClientActivityAtMillis mtime of `.git/index` — the sync client touches it on
     *   every status check, so it shows the client is alive even when there is nothing to pull
     * @param problem an unfinished merge/rebase or a branch that drifted from the remote
     */
    data class Tracked(
        val lastChangeAtMillis: Long,
        val lastAction: String?,
        val lastFetchAtMillis: Long?,
        val branch: String?,
        val lastClientActivityAtMillis: Long? = null,
        val problem: Problem = Problem.None
    ) : GitSyncStatus

    /** Trouble the sync client left behind, detectable from `.git` alone. */
    enum class Problem {
        /** Nothing unusual: no half-finished operation, local matches the remote ref we know of. */
        None,

        /** `MERGE_HEAD` present — a merge stopped, in practice almost always a conflict. */
        Conflict,

        /** `rebase-merge` / `rebase-apply` present — a rebase was interrupted. */
        RebaseInProgress,

        /** Local branch and the remote-tracking ref point at different commits. */
        Diverged
    }
}
