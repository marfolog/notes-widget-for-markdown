package com.marfolog.noteswidgetformarkdown.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.marfolog.noteswidgetformarkdown.data.parser.GitBranchRelation
import com.marfolog.noteswidgetformarkdown.data.parser.GitConfigParser
import com.marfolog.noteswidgetformarkdown.data.parser.GitPackedRefsParser
import com.marfolog.noteswidgetformarkdown.data.parser.GitReflogParser
import com.marfolog.noteswidgetformarkdown.domain.model.GitSyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Reads git sync metadata straight from the vault's `.git` folder over SAF.
 *
 * We cannot run git on Android, and we do not want to — the vault is synced by
 * some other client (obsidian-git, GitSync…). Everything we need is plain text:
 *  - `.git/logs/HEAD` — reflog, last line is the most recent commit/pull
 *  - `.git/logs/refs/remotes/<remote>/<branch>` — moves on every fetch that brought something and on every push
 *  - `.git/FETCH_HEAD` — its mtime is the last time the client reached the remote
 *  - `.git/HEAD` — current branch; `.git/config` — which remote it tracks
 */
class GitSyncStatusReader(
    private val context: Context,
    private val reflogParser: GitReflogParser = GitReflogParser(),
    private val packedRefsParser: GitPackedRefsParser = GitPackedRefsParser(),
    private val configParser: GitConfigParser = GitConfigParser()
) {

    /**
     * SAF only ever grants a subtree, and there is no way to walk above it. So instead of
     * trusting a single preference key, we look for `.git` in every folder the user has
     * granted us — whichever of them happens to be the repo root wins.
     */
    suspend fun read(vaultUri: String?): GitSyncStatus = withContext(Dispatchers.IO) {
        val candidates = (listOfNotNull(vaultUri.takeUnless { it.isNullOrEmpty() }) +
            context.contentResolver.persistedUriPermissions
                // Only folders can hold a .git. A single-file grant (an extra note) would make
                // fromTreeUri throw and turn "no git anywhere" into a red "unreadable" error.
                .filter { it.isReadPermission && DocumentsContract.isTreeUri(it.uri) }
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

            val reflog = findPath(gitDir, "$LOGS_DIR/$HEAD_FILE")
            val headLines = reflog?.let { readLines(it) }.orEmpty()
            val entry = headLines.lastOrNull()

            val lastChangeAtMillis = entry?.timestampMillis
                ?: reflog?.lastModified()?.takeIf { it > 0 }
                ?: return@runCatching GitSyncStatus.Unavailable("No git history yet")

            val branch = gitDir.findFile(HEAD_FILE)
                ?.let { readText(it) }
                ?.let { reflogParser.parseBranch(it) }
            val remote = branch?.let { remoteOf(gitDir, it) } ?: GitConfigParser.DEFAULT_REMOTE
            val remoteLog = branch
                ?.let { findPath(gitDir, "$LOGS_DIR/$REFS_DIR/$REMOTES_DIR/$remote/$it") }
                ?.let { readLines(it) }
                .orEmpty()

            val relation = branch?.let { relationOf(gitDir, it, remote, headLines, remoteLog) }

            GitSyncStatus.Tracked(
                lastChangeAtMillis = lastChangeAtMillis,
                lastAction = entry?.let { reflogParser.normalizeAction(it.rawAction) },
                lastFetchAtMillis = gitDir.findFile(FETCH_HEAD_FILE)?.lastModified()?.takeIf { it > 0 },
                branch = branch,
                lastClientActivityAtMillis = gitDir.findFile(INDEX_FILE)
                    ?.lastModified()
                    ?.takeIf { it > 0 },
                lastRemoteRefUpdateAtMillis = remoteLog.lastOrNull()?.timestampMillis,
                lastPullAtMillis = headLines
                    .lastOrNull { reflogParser.isRemoteContact(it.rawAction) }
                    ?.timestampMillis,
                unpushedSinceMillis = (relation as? GitBranchRelation.Ahead)?.unpushedSinceMillis,
                problem = detectProblem(gitDir, relation)
            )
        }.getOrElse { e ->
            Log.w(TAG, "Could not read git status from $treeUri", e)
            GitSyncStatus.Unavailable(e.message ?: "Unknown error")
        }

    /**
     * A half-finished merge or rebase is what a sync client leaves behind when it hits
     * a conflict and gives up, which is exactly the case we want the widget to shout about.
     * Fetched-but-not-merged is the client doing its job, so it is no problem.
     */
    private fun detectProblem(gitDir: DocumentFile, relation: GitBranchRelation?): GitSyncStatus.Problem = when {
        gitDir.findFile(MERGE_HEAD_FILE) != null -> GitSyncStatus.Problem.Conflict
        gitDir.findFile(REBASE_MERGE_DIR) != null ||
            gitDir.findFile(REBASE_APPLY_DIR) != null -> GitSyncStatus.Problem.RebaseInProgress
        relation is GitBranchRelation.Ahead -> GitSyncStatus.Problem.Ahead
        relation is GitBranchRelation.Diverged -> GitSyncStatus.Problem.Diverged
        else -> GitSyncStatus.Problem.None
    }

    /**
     * Compares `refs/heads/<branch>` with the remote-tracking ref. Unknown refs mean
     * "no evidence of a problem" — we never invent a warning out of missing data.
     */
    private fun relationOf(
        gitDir: DocumentFile,
        branch: String,
        remote: String,
        headLines: List<GitReflogParser.Line>,
        remoteLog: List<GitReflogParser.Line>
    ): GitBranchRelation? {
        val packed = gitDir.findFile(PACKED_REFS_FILE)
            ?.let { readText(it) }
            ?.let { packedRefsParser.parse(it) }
            .orEmpty()

        val local = resolveRef(gitDir, "$REFS_DIR/$HEADS_DIR/$branch", packed)
        val remoteSha = resolveRef(gitDir, "$REFS_DIR/$REMOTES_DIR/$remote/$branch", packed)

        if (!GitBranchRelation.isObjectId(local) || !GitBranchRelation.isObjectId(remoteSha)) {
            return null
        }
        if (local.equals(remoteSha, ignoreCase = true)) {
            return GitBranchRelation.InSync
        }
        val branchLog = findPath(gitDir, "$LOGS_DIR/$REFS_DIR/$HEADS_DIR/$branch")
            ?.let { readLines(it) }
            ?: headLines
        return GitBranchRelation.classify(local, remoteSha, branchLog, remoteLog)
    }

    private fun remoteOf(gitDir: DocumentFile, branch: String): String =
        gitDir.findFile(CONFIG_FILE)
            ?.let { readText(it) }
            ?.let { configParser.remoteFor(it, branch) }
            ?: GitConfigParser.DEFAULT_REMOTE

    /** Loose ref first, packed-refs second — git's own precedence. */
    private fun resolveRef(
        gitDir: DocumentFile,
        refPath: String,
        packed: Map<String, String>
    ): String? {
        val loose = findPath(gitDir, refPath)
            ?.let { readText(it) }
            ?.trim()

        return loose?.takeIf { it.isNotEmpty() } ?: packed[refPath]
    }

    private fun findPath(root: DocumentFile, path: String): DocumentFile? =
        path.split('/').fold(root as DocumentFile?) { dir, segment -> dir?.findFile(segment) }

    /** Reflogs grow forever and only their end matters, so only the last [TAIL_BYTES] are read. */
    private fun readLines(file: DocumentFile): List<GitReflogParser.Line> {
        val length = file.length()
        val truncated = length > TAIL_BYTES
        val text = runCatching {
            context.contentResolver.openInputStream(file.uri)?.use { stream ->
                if (truncated) skipFully(stream, length - TAIL_BYTES)
                String(stream.readBytes(), Charsets.UTF_8)
            }
        }.getOrNull() ?: return emptyList()
        return reflogParser.parseLines(text, partialFirstLine = truncated)
    }

    private fun skipFully(stream: InputStream, bytes: Long) {
        var left = bytes
        while (left > 0) {
            val skipped = stream.skip(left)
            if (skipped <= 0) {
                return
            }
            left -= skipped
        }
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
        const val CONFIG_FILE = "config"
        const val TAIL_BYTES = 64L * 1024
        const val PACKED_REFS_FILE = "packed-refs"
    }
}
