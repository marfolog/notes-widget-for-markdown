package com.marfolog.noteswidgetformarkdown.data.repository

import com.marfolog.noteswidgetformarkdown.domain.model.FolderScan
import com.marfolog.noteswidgetformarkdown.domain.model.SubfolderCount

/** One child of a folder as the provider reports it. [name] is null when the provider does not say. */
internal data class DocEntry(val id: String, val name: String?, val isDirectory: Boolean)

/** Lists the children of one folder; null when it cannot be read, whatever the reason. */
internal fun interface DirectoryLister {
    fun list(directoryId: String): List<DocEntry>?
}

internal data class ScanLimits(
    val maxDepth: Int = 4,
    val maxItems: Int = 2_000,
    val maxMillis: Long = 2_000
)

/**
 * Looks through a folder to count Markdown files, never to read them. Everything that can go
 * wrong with a provider (a revoked grant, a folder deleted in the middle, a name that is not
 * there, a tree that is far too big, a link back to a folder already seen) ends in a smaller
 * result, never in an exception: the caller only wants to explain an empty folder.
 */
internal object FolderScanner {

    private class Pending(val id: String, val name: String?, val path: String, val depth: Int)

    /** A folder below the root and the Markdown files directly in it. */
    private class Folder(val id: String, val path: String, var count: Int = 0)

    fun scan(
        rootId: String,
        rootName: String?,
        lister: DirectoryLister,
        limits: ScanLimits = ScanLimits(),
        clock: () -> Long = System::currentTimeMillis
    ): FolderScan {
        val startedAt = clock()
        var items = 0
        var truncated = false
        var directNotes = 0
        var otherFiles = 0
        val folders = mutableListOf<Folder>()
        val seen = HashSet<String>().apply { add(rootId) }
        val queue = ArrayDeque<Pending>().apply { add(Pending(rootId, rootName, "", 0)) }
        var rootRead = false

        while (queue.isNotEmpty()) {
            if (items >= limits.maxItems || clock() - startedAt > limits.maxMillis) {
                truncated = true
                break
            }
            val dir = queue.removeFirst()
            val children = safeList(lister, dir.id)
            if (children == null) {
                // A folder deeper down that cannot be read is skipped; the root is the one that matters.
                if (dir.depth == 0) return FolderScan.Unreadable
                // What is in it stays unknown, so what was counted is at least that and no more.
                truncated = true
                continue
            }
            if (dir.depth == 0) rootRead = true
            val folder = if (dir.depth == 0) null else Folder(dir.id, dir.path).also { folders.add(it) }

            for (child in children) {
                if (items >= limits.maxItems) {
                    truncated = true
                    break
                }
                items++
                val name = child.name
                if (name.isNullOrEmpty()) continue
                if (child.isDirectory) {
                    if (isHidden(name) || dir.depth >= limits.maxDepth || !seen.add(child.id)) continue
                    val path = if (dir.path.isEmpty()) name else "${dir.path}/$name"
                    queue.add(Pending(child.id, name, path, dir.depth + 1))
                } else if (isMarkdown(name)) {
                    if (isFolderNote(name, dir.name)) continue
                    if (folder == null) directNotes++ else folder.count++
                } else {
                    otherFiles++
                }
            }
        }

        return FolderScan(
            readable = rootRead,
            directNotes = directNotes,
            otherFiles = otherFiles,
            subfolders = folders.filter { it.count > 0 }.map { SubfolderCount(it.id, it.path, it.count) },
            truncated = truncated
        )
    }

    private fun safeList(lister: DirectoryLister, id: String): List<DocEntry>? =
        try {
            lister.list(id)
        } catch (e: Exception) {
            null
        }

    /** `.obsidian`, `.trash`, `.git` and every other folder the user does not see as theirs. */
    fun isHidden(folderName: String): Boolean = folderName.startsWith(".")

    fun isMarkdown(fileName: String): Boolean = fileName.endsWith(".md", ignoreCase = true)

    /**
     * Obsidian's folder note: a file named exactly like its folder (Projects/Projects.md). Obsidian
     * shows it as the folder itself, so the widget leaves it out and so does the count.
     */
    fun isFolderNote(fileName: String, folderName: String?): Boolean =
        folderName != null && fileName.removeSuffix(".md").equals(folderName, ignoreCase = true)
}
