package com.marfolog.noteswidgetformarkdown.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.marfolog.noteswidgetformarkdown.data.parser.MarkdownFastNoteInserter
import com.marfolog.noteswidgetformarkdown.data.parser.MarkdownPreviewFormatter
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class FileRepositoryImpl(
    private val context: Context,
    private val previewFormatter: MarkdownPreviewFormatter = MarkdownPreviewFormatter(),
    private val fastNoteInserter: MarkdownFastNoteInserter = MarkdownFastNoteInserter()
) : FileRepository {

    private val contentResolver get() = context.contentResolver

    override fun getNotes(folderUri: String): Flow<List<NoteSummary>> = flow {
        val startedAt = System.currentTimeMillis()
        val treeUri = Uri.parse(folderUri)

        // Check if we still hold persistable permission for this URI
        val hasPermission = contentResolver.persistedUriPermissions.any { perm ->
            perm.uri == treeUri && perm.isReadPermission
        }
        if (!hasPermission) {
            throw SecurityException("Folder permission was revoked. Please re-select your notes folder.")
        }

        val folder = DocumentFile.fromTreeUri(context, treeUri)
        if (folder == null || !folder.exists()) {
            throw IllegalStateException("Folder not found. Please re-select your notes folder.")
        }

        // Obsidian folder note: a file named exactly like its parent folder
        // (e.g. Projects/Projects.md). Obsidian hides it as the
        // folder itself, so hide it here too to match what the user sees.
        val folderName = folder.name
        val notes = folder.listFiles()
            .filter { it.isFile && it.name?.endsWith(".md", ignoreCase = true) == true }
            .filterNot { docFile ->
                folderName != null &&
                    docFile.name?.removeSuffix(".md").equals(folderName, ignoreCase = true)
            }
            .mapNotNull { docFile -> fileToNoteSummary(docFile) }

        Log.d(TAG, "Loaded ${notes.size} notes in ${System.currentTimeMillis() - startedAt}ms")
        emit(notes)
    }.flowOn(Dispatchers.IO)

    override suspend fun createNote(
        folderUri: String,
        title: String,
        content: String
    ): Result<Boolean> = runCatching {
        val treeUri = Uri.parse(folderUri)
        val folder = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@runCatching false

        val fileName = "$title.md"
        val newFile = folder.createFile("text/markdown", fileName)
            ?: return@runCatching false

        contentResolver.openOutputStream(newFile.uri)?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
        } ?: return@runCatching false

        true
    }

    override suspend fun appendFastNote(fileUri: String, noteText: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(fileUri)
            val originalContent = contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: return@runCatching false

            val updatedContent = fastNoteInserter.insert(originalContent, noteText)
            contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(updatedContent.toByteArray(Charsets.UTF_8))
            } ?: return@runCatching false

            true
        }
    }

    override suspend fun deleteNote(fileUri: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val deleted = DocumentsContract.deleteDocument(contentResolver, Uri.parse(fileUri))
            Log.d(TAG, "Delete of $fileUri returned $deleted")
            deleted
        }
    }

    private fun fileToNoteSummary(docFile: DocumentFile): NoteSummary? {
        return runCatching {
            val name = docFile.name ?: return null
            val title = name.removeSuffix(".md")
            val preview = readPreview(docFile.uri)
            val lastModified = docFile.lastModified()

            NoteSummary(
                id = docFile.uri.toString(),
                title = title,
                preview = preview,
                lastModified = lastModified,
                fileUri = docFile.uri.toString(),
                fileName = name
            )
        }.getOrNull()
    }

    private fun readPreview(uri: Uri): String {
        return runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    val lines = mutableListOf<String>()
                    var line: String?
                    while (reader.readLine().also { line = it } != null && lines.size < PREVIEW_SOURCE_LINE_COUNT) {
                        lines.add(line.orEmpty())
                    }
                    previewFormatter.format(lines.joinToString("\n"))
                }
            } ?: ""
        }.getOrDefault("")
    }

    companion object {
        private const val TAG = "FileRepository"
        private const val PREVIEW_SOURCE_LINE_COUNT = 36
    }
}
