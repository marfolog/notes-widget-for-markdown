package com.marfolog.noteswidgetformarkdown.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.marfolog.noteswidgetformarkdown.data.parser.GitPackedRefsParser
import com.marfolog.noteswidgetformarkdown.data.parser.GitReflogParser
import com.marfolog.noteswidgetformarkdown.domain.model.GitSyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads git sync metadata straight from the vault's `.git` folder over SAF.
 *
 * We cannot run git on Android, and we do not want to — the vault is synced by
 * some other client (obsidian-git, GitSync…). Everything we need is plain text:
 *  - `.git/logs/HEAD` — reflog, last line is the most recent commit/pull
 *  - `.git/FETCH_HEAD` — its mtime is the last time the client reached the remote
 *  - `.git/HEAD` — current branch
 */
class GitSyncStatusReader(
    private val context: Context,
    private val reflogParser: GitReflogParser = GitReflogParser(),
    private val packedRefsParser: GitPackedRefsParser = GitPackedRefsParser()
) {

    /**
     * SAF only ever grants a subtree, and there is no way to walk above it. So instead of
     * trusting a single preference key, we look for `.git` in every folder the user has
     * granted us — whichever of them happens to be the repo root wins.
     */
    suspend fun read(vaultUri: String?): GitSyncStatus = withContext(Dispatchers.IO) {
        val candidates = (listOfNotNull(vaultUri.takeUnless { it.isNullOrEmpty() }) +
            context.contentResolver.persistedUriPermissions
                .filter { it.isReadPermission }
                .map { it.uri.toString() })
            .distinct()

        // Repozitar bez jedineho commitu neni duvod prestat hledat — dal muze byt ten pravy.
        // Cteme lenive, at pri kazdem obnoveni widgetu neprochazime vsechny povolene slozky.
        var fallback: GitSyncStatus? = null
        for (uri in candidates) {
            val status = readFrom(uri) ?: continue
            if (status !is GitSyncStatus.Unavailable) return@withContext status
            if (fallback == null) fallback = status
        }
        fallback ?: GitSyncStatus.NotTracked
    }

    /** Returns null when this tree simply has no `.git`, so the next candidate can be tried. */
    private fun readFrom(treeUri: String): GitSyncStatus? =
        runCatching {
            val vault = DocumentFile.fromTreeUri(context, Uri.parse(treeUri))
                ?: return@runCatching null
            val gitDir = vault.findFile(GIT_DIR)?.takeIf { it.isDirectory }
                ?: return@runCatching null

            val reflog = gitDir.findFile(LOGS_DIR)?.findFile(HEAD_FILE)
            val entry = reflog
                ?.let { readText(it) }
                ?.let { reflogParser.parseLastEntry(it) }

            val lastChangeAtMillis = entry?.timestampMillis
                ?: reflog?.lastModified()?.takeIf { it > 0 }
                ?: return@runCatching GitSyncStatus.Unavailable("No git history yet")

            val branch = gitDir.findFile(HEAD_FILE)
                ?.let { readText(it) }
                ?.let { reflogParser.parseBranch(it) }

            GitSyncStatus.Tracked(
                lastChangeAtMillis = lastChangeAtMillis,
                lastAction = entry?.action,
                lastFetchAtMillis = gitDir.findFile(FETCH_HEAD_FILE)?.lastModified()?.takeIf { it > 0 },
                branch = branch,
                lastClientActivityAtMillis = gitDir.findFile(INDEX_FILE)
                    ?.lastModified()
                    ?.takeIf { it > 0 },
                problem = detectProblem(gitDir, branch)
            )
        }.getOrElse { e ->
            Log.w(TAG, "Could not read git status from $treeUri", e)
            GitSyncStatus.Unavailable(e.message ?: "Unknown error")
        }

    /**
     * Everything here is a file-existence or string-compare check — no git needed.
     * A half-finished merge or rebase is what a sync client leaves behind when it hits
     * a conflict and gives up, which is exactly the case we want the widget to shout about.
     */
    private fun detectProblem(gitDir: DocumentFile, branch: String?): GitSyncStatus.Problem = when {
        gitDir.findFile(MERGE_HEAD_FILE) != null -> GitSyncStatus.Problem.Conflict
        gitDir.findFile(REBASE_MERGE_DIR) != null ||
            gitDir.findFile(REBASE_APPLY_DIR) != null -> GitSyncStatus.Problem.RebaseInProgress
        branch != null && hasDiverged(gitDir, branch) -> GitSyncStatus.Problem.Diverged
        else -> GitSyncStatus.Problem.None
    }

    /**
     * Compares `refs/heads/<branch>` with `refs/remotes/origin/<branch>`. Unknown refs mean
     * "no evidence of a problem" — we never invent a warning out of missing data.
     */
    private fun hasDiverged(gitDir: DocumentFile, branch: String): Boolean {
        val packed = gitDir.findFile(PACKED_REFS_FILE)
            ?.let { readText(it) }
            ?.let { packedRefsParser.parse(it) }
            .orEmpty()

        val local = resolveRef(gitDir, "$REFS_DIR/$HEADS_DIR/$branch", packed)
        val remote = resolveRef(gitDir, "$REFS_DIR/$REMOTES_DIR/$ORIGIN_DIR/$branch", packed)

        if (local.isNullOrEmpty() || remote.isNullOrEmpty()) {
            return false
        }
        return !local.equals(remote, ignoreCase = true)
    }

    /** Loose ref first, packed-refs second — git's own precedence. */
    private fun resolveRef(
        gitDir: DocumentFile,
        refPath: String,
        packed: Map<String, String>
    ): String? {
        val loose = refPath.split('/')
            .fold(gitDir as DocumentFile?) { dir, segment -> dir?.findFile(segment) }
            ?.let { readText(it) }
            ?.trim()

        return loose?.takeIf { it.isNotEmpty() } ?: packed[refPath]
    }

    private fun readText(file: DocumentFile): String? = runCatching {
        context.contentResolver.openInputStream(file.uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        }
    }.getOrNull()

    private companion object {
        const val TAG = "GitSyncStatusReader"
        const val GIT_DIR = ".git"
        const val LOGS_DIR = "logs"
        const val HEAD_FILE = "HEAD"
        const val FETCH_HEAD_FILE = "FETCH_HEAD"
        const val INDEX_FILE = "index"
        const val MERGE_HEAD_FILE = "MERGE_HEAD"
        const val REBASE_MERGE_DIR = "rebase-merge"
        const val REBASE_APPLY_DIR = "rebase-apply"
        const val REFS_DIR = "refs"
        const val HEADS_DIR = "heads"
        const val REMOTES_DIR = "remotes"
        const val ORIGIN_DIR = "origin"
        const val PACKED_REFS_FILE = "packed-refs"
    }
}
