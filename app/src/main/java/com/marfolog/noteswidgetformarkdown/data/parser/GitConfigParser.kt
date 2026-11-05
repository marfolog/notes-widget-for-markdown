package com.marfolog.noteswidgetformarkdown.data.parser

/** Reads which remote a branch tracks from `.git/config`: `[branch "main"]` with `remote = name`. */
class GitConfigParser {

    fun remoteFor(config: String, branch: String): String {
        var inBranchSection = false
        for (raw in config.lineSequence()) {
            val line = raw.trim()
            if (line.startsWith("[")) {
                inBranchSection = line == """[branch "$branch"]"""
                continue
            }
            if (!inBranchSection) {
                continue
            }
            val key = line.substringBefore('=').trim()
            if (key.equals(REMOTE_KEY, ignoreCase = true) && line.contains('=')) {
                return line.substringAfter('=').trim().takeIf { it.isNotEmpty() } ?: DEFAULT_REMOTE
            }
        }
        return DEFAULT_REMOTE
    }

    companion object {
        const val DEFAULT_REMOTE = "origin"
        private const val REMOTE_KEY = "remote"
    }
}
