package com.marfolog.noteswidgetformarkdown.data.parser

/**
 * Parses `.git/logs/HEAD` (the HEAD reflog) — a plain text file every git client
 * appends to, one line per HEAD move:
 *
 * ```
 * <old-sha> <new-sha> Name <mail@example.com> 1735689600 +0100\tpull: Fast-forward
 * ```
 *
 * Only the last line matters: it is the most recent commit / pull / checkout.
 */
class GitReflogParser {

    data class Entry(
        val timestampMillis: Long,
        val action: String?
    )

    fun parseLastEntry(reflog: String): Entry? {
        val lastLine = reflog
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .split("\n")
            .lastOrNull { it.isNotBlank() }
            ?: return null

        val match = LINE_REGEX.find(lastLine) ?: return null
        val epochSeconds = match.groupValues[1].toLongOrNull() ?: return null
        val rawAction = match.groupValues.getOrNull(2)
            ?.substringBefore(':')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        return Entry(timestampMillis = epochSeconds * 1000L, action = normalizeAction(rawAction))
    }

    /**
     * Clients word the same operation differently — obsidian-git (isomorphic-git) writes
     * `Fast-Forward: Setting refs/heads/main to id: …` where CLI git writes `pull: Fast-forward`.
     * Both mean "pulled from the remote".
     */
    private fun normalizeAction(rawAction: String?): String? {
        val action = rawAction?.lowercase() ?: return null
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

    /** Resolves `.git/HEAD` (`ref: refs/heads/main`) to a branch name. */
    fun parseBranch(head: String): String? {
        val trimmed = head.trim()
        if (!trimmed.startsWith(HEAD_REF_PREFIX)) {
            return null
        }
        return trimmed
            .removePrefix(HEAD_REF_PREFIX)
            .substringAfterLast('/')
            .takeIf { it.isNotEmpty() }
    }

    private companion object {
        // …<epoch-seconds> <timezone><TAB or spaces><action>: <message>
        val LINE_REGEX = Regex("""\s(\d{9,})\s[+-]\d{4}\s+(.*)$""")
        const val HEAD_REF_PREFIX = "ref: refs/heads/"
    }
}
