package com.marfolog.noteswidgetformarkdown.data.parser

/**
 * How the local branch stands to its remote-tracking ref, decided from two reflogs alone — no
 * object database, no graph walk.
 */
sealed interface GitBranchRelation {

    data object InSync : GitBranchRelation

    /** Only plain commits sit on top of the remote tip; the first of them was made at this time. */
    data class Ahead(val unpushedSinceMillis: Long) : GitBranchRelation

    /** The remote moved on and the client fetched it, but has not merged it yet. */
    data object Behind : GitBranchRelation

    data object Diverged : GitBranchRelation

    companion object {
        private const val PLAIN_COMMIT = "commit"

        /** SHA-1 (40) or SHA-256 (64) hex, the only things a ref may point at. */
        private val OBJECT_ID = Regex("^([0-9a-fA-F]{40}|[0-9a-fA-F]{64})$")

        fun isObjectId(value: String?): Boolean = value != null && OBJECT_ID.matches(value)

        /**
         * Null when either ref is missing or is not an object id at all, e.g. a half-written or
         * corrupted file. Damaged data is no evidence of unpushed work, so it must not raise one.
         */
        fun classify(
            localSha: String?,
            remoteSha: String?,
            branchLog: List<GitReflogParser.Line>,
            remoteLog: List<GitReflogParser.Line>
        ): GitBranchRelation? = when {
            !isObjectId(localSha) || !isObjectId(remoteSha) -> null
            localSha!!.equals(remoteSha, ignoreCase = true) -> InSync
            else -> unpushedSince(localSha, remoteSha!!, branchLog)?.let(::Ahead)
                ?: if (remoteLog.any { it.newSha.equals(localSha, ignoreCase = true) }) Behind else Diverged
        }

        /**
         * Walks back from the newest entry while each one continues the previous (`old` equals the
         * earlier `new`) and is a plain `commit`. Amend, merge, reset or a gap mean we cannot vouch
         * for the history between local and remote, so there is no answer.
         */
        private fun unpushedSince(
            localSha: String,
            remoteSha: String,
            branchLog: List<GitReflogParser.Line>
        ): Long? {
            var index = branchLog.lastIndex
            if (index < 0 || !branchLog[index].newSha.equals(localSha, ignoreCase = true)) {
                return null
            }
            while (index >= 0) {
                val entry = branchLog[index]
                if (entry.rawAction != PLAIN_COMMIT) {
                    return null
                }
                if (entry.oldSha.equals(remoteSha, ignoreCase = true)) {
                    return entry.timestampMillis
                }
                val previous = branchLog.getOrNull(index - 1) ?: return null
                if (!previous.newSha.equals(entry.oldSha, ignoreCase = true)) {
                    return null
                }
                index--
            }
            return null
        }
    }
}
