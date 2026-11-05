package com.marfolog.noteswidgetformarkdown.data.parser

/**
 * Parses `.git/logs/HEAD` (the HEAD reflog) — a plain text file every git client
 * appends to, one line per HEAD move:
 *
 * ```
 * <old-sha> <new-sha> Name <mail@example.com> 1735689600 +0100\tpull: Fast-forward
 * ```
 *
 * The last line is the most recent commit / pull / checkout; the old/new shas of the lines let
 * [GitBranchRelation] tell unpushed commits from unmerged ones without reading any git object.
 */
class GitReflogParser {

    data class Entry(
        val timestampMillis: Long,
        val action: String?
    )

    /** One reflog line with everything the ahead/behind decision needs. [rawAction] is as written. */
    data class Line(
        val oldSha: String,
        val newSha: String,
        val timestampMillis: Long,
        val rawAction: String
    )

    fun parseLastEntry(reflog: String): Entry? = parseLines(reflog).lastOrNull()?.let {
        Entry(timestampMillis = it.timestampMillis, action = normalizeAction(it.rawAction))
    }

    /**
     * [partialFirstLine] is for a reflog read from the middle of the file: whatever comes before
     * the first newline is the end of a line we never saw the start of.
     */
    fun parseLines(reflog: String, partialFirstLine: Boolean = false): List<Line> {
        val lines = reflog
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .split("\n")
        return lines
            .drop(if (partialFirstLine) 1 else 0)
            .mapNotNull { parseLine(it) }
    }

    private fun parseLine(line: String): Line? {
        if (line.isBlank()) {
            return null
        }
        val match = LINE_REGEX.find(line) ?: return null
        val epochSeconds = match.groupValues[3].toLongOrNull() ?: return null
        val rawAction = match.groupValues[4].substringBefore(':').trim()
        return Line(
            oldSha = match.groupValues[1],
            newSha = match.groupValues[2],
            timestampMillis = epochSeconds * 1000L,
            rawAction = rawAction
        )
    }

    /**
     * Clients word the same operation differently — obsidian-git (isomorphic-git) writes
     * `Fast-Forward: Setting refs/heads/main to id: …` where CLI git writes `pull: Fast-forward`.
     * Both mean "pulled from the remote".
     */
    fun normalizeAction(rawAction: String?): String? {
        val action = rawAction?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        return when {
            action.startsWith("fast-forward") -> "pull"
            action.startsWith("pull") -> "pull"
            action.startsWith("commit") -> "commit"
            action.startsWith("merge") -> "merge"
            action.startsWith("clone") -> "clone"
            action.startsWith("checkout") -> "checkout"
            else -> rawAction
        }
    }

    /** A pull, a fast-forward or a clone can only happen after the client talked to the remote. */
    fun isRemoteContact(rawAction: String): Boolean = when (normalizeAction(rawAction)) {
        "pull", "clone" -> true
        else -> false
    }

    /** Resolves `.git/HEAD` (`ref: refs/heads/feature/x`) to the branch name, slashes intact. */
    fun parseBranch(head: String): String? {
        val trimmed = head.trim()
        if (!trimmed.startsWith(HEAD_REF_PREFIX)) {
            return null
        }
        return trimmed
            .removePrefix(HEAD_REF_PREFIX)
            .takeIf { it.isNotEmpty() }
    }

    private companion object {
        // <old> <new> Name <mail> <epoch-seconds> <timezone><TAB or spaces><action>: <message>
        val LINE_REGEX = Regex("""^(\S+)\s+(\S+)\s.*?\s(\d{9,})\s[+-]\d{4}\s+(.*)$""")
        const val HEAD_REF_PREFIX = "ref: refs/heads/"
    }
}
