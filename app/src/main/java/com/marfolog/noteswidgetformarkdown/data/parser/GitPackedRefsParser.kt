package com.marfolog.noteswidgetformarkdown.data.parser

/**
 * Parses `.git/packed-refs`. Git packs loose refs away during `git gc`, so on a repo maintained
 * by CLI git the files under `refs/heads` may not exist at all — the ref lives here instead:
 *
 * ```
 * # pack-refs with: peeled fully-peeled sorted
 * 8cafb2f113d13d06f9d1229f2d5b9c116bfbb79a refs/heads/main
 * aa9440743a873c07a8b9706b3514cafca160708a refs/remotes/origin/main
 * ^1c2b3d4e5f60718293a4b5c6d7e8f9012345678a
 * ```
 *
 * A loose ref always wins over a packed one, which is git's own rule.
 */
class GitPackedRefsParser {

    fun parse(packedRefs: String): Map<String, String> = packedRefs
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .split("\n")
        .mapNotNull { line ->
            val trimmed = line.trim()
            // '#' is the header, '^' is the peeled tag target — neither is a ref of its own.
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("^")) {
                return@mapNotNull null
            }

            val sha = trimmed.substringBefore(' ').takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val ref = trimmed.substringAfter(' ', "").trim().takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            ref to sha
        }
        .toMap()
}
