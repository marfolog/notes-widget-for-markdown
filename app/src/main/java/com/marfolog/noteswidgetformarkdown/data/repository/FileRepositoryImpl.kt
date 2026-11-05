package com.marfolog.noteswidgetformarkdown.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.marfolog.noteswidgetformarkdown.data.parser.MarkdownFastNoteInserter
import com.marfolog.noteswidgetformarkdown.domain.model.CreatedFile
import com.marfolog.noteswidgetformarkdown.domain.model.FastNoteTarget
import com.marfolog.noteswidgetformarkdown.domain.model.FastNoteStyle
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.data.parser.MarkdownPreviewFormatter
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class FileRepositoryImpl(
    private val context: Context,
    private val previewFormatter: MarkdownPreviewFormatter = MarkdownPreviewFormatter(),
    private val fastNoteInserter: MarkdownFastNoteInserter = MarkdownFastNoteInserter()
) : FileRepository {

    private val contentResolver get() = context.contentResolver

    override fun getNotes(folderUri: String, extraFileUris: List<String>): Flow<List<NoteSummary>> = flow {
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

        AppLog.d("Files", "Loaded ${notes.size} notes in ${System.currentTimeMillis() - startedAt}ms from $folderUri")
        AppLog.d("Files", notes.joinToString { "${it.fileName}@${it.lastModified}" })

        // An extra note that went missing is skipped, not an error: the folder is what the
        // widget is about, and one moved file must not blank the whole thing.
        val extraNotes = extraFileUris.mapNotNull { getExtraNote(it).getOrNull() }
        if (extraFileUris.isNotEmpty()) {
            AppLog.d("Files", "Loaded ${extraNotes.size} of ${extraFileUris.size} extra notes")
        }
        emit(mergeFolderAndExtraNotes(notes, extraNotes))
    }.flowOn(Dispatchers.IO)

    override suspend fun getExtraNote(fileUri: String): Result<NoteSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(fileUri)
            // Same check the folder gets: without a persisted grant the reads below would only
            // work until the process dies, and then fail somewhere less obvious.
            val hasPermission = contentResolver.persistedUriPermissions.any { perm ->
                perm.uri == uri && perm.isReadPermission
            }
            if (!hasPermission) {
                throw SecurityException("Access to an extra note was revoked")
            }
            val docFile = DocumentFile.fromSingleUri(context, uri)
                ?: throw IllegalStateException("Extra note could not be resolved")
            if (!docFile.exists()) {
                throw java.io.FileNotFoundException("Extra note no longer exists")
            }
            fileToNoteSummary(docFile)?.copy(isExtra = true)
                ?: throw IllegalStateException("Extra note could not be read")
        }
    }

    override suspend fun createNote(
        folderUri: String,
        title: String,
        content: String
    ): Result<String> = runCatching {
        val treeUri = Uri.parse(folderUri)
        val folder = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalStateException("Folder not found")

        val fileName = "$title.md"
        // The provider may not return a file named exactly this — on a name collision (which a
        // stale listFiles() cache can hide from the caller) it is free to pick its own name
        // instead of failing. The uri it hands back is what actually exists; the name asked for
        // might not.
        val newFile = folder.createFile("text/markdown", fileName)
            ?: throw IllegalStateException("Provider did not create the file")

        contentResolver.openOutputStream(newFile.uri)?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("Could not open the new file for writing")
        // A folder that was empty has a note now, which the next look into it must find.
        FolderDiagnoser.forget(folderUri)

        newFile.uri.toString()
    }

    override suspend fun createFastNoteFile(folderUri: String, baseName: String): Result<CreatedFile> =
        withContext(Dispatchers.IO) {
            runCatching {
                val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
                    ?.takeIf { it.exists() }
                    ?: throw IllegalStateException("Folder not found")
                // Looked up fresh instead of trusting a list the screen may have loaded a while ago.
                val taken = folder.listFiles().mapNotNull { it.name }
                val fileName = FastNoteTarget.freeFileName(baseName, taken)
                val created = folder.createFile("text/markdown", fileName)
                    ?: throw IllegalStateException("Provider did not create the file")
                // The provider may have picked another name after all; the file's own name is the truth.
                FolderDiagnoser.forget(folderUri)
                CreatedFile(uri = created.uri.toString(), name = created.name ?: fileName)
            }
        }

    override suspend fun fileSize(fileUri: String): Long? = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.query(Uri.parse(fileUri), arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
            }
        }.getOrNull()
    }

    override suspend fun appendFastNote(fileUri: String, noteText: String, style: FastNoteStyle): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(fileUri)
            val originalContent = contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: return@runCatching false

            val updatedContent = fastNoteInserter.insert(originalContent, noteText, style)
            val updatedBytes = updatedContent.toByteArray(Charsets.UTF_8)

            // "wt" truncates first, so a failure between truncating and writing would leave the
            // note empty — and the target is the file someone appends to every day. Keep the old
            // bytes in cache until the new ones are safely written, and put them back if not.
            val rescue = File(context.cacheDir, "fast-note-rescue.md")
            rescue.writeBytes(originalContent.toByteArray(Charsets.UTF_8))

            val written = runCatching {
                contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                    stream.write(updatedBytes)
                    stream.flush()
                } ?: return@runCatching false
                true
            }

            if (written.isFailure) {
                runCatching {
                    contentResolver.openOutputStream(uri, "wt")?.use { it.write(rescue.readBytes()) }
                }.onFailure { restoreError ->
                    Log.e(TAG, "Fast note write failed and the note could not be restored", restoreError)
                }
                rescue.delete()
                throw written.exceptionOrNull() ?: IllegalStateException("Fast note write failed")
            }

            rescue.delete()
            true
        }
    }

    override suspend fun deleteNote(fileUri: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(fileUri)
            // Folder notes are always listed through the granted tree, so their uris are tree
            // uris. A plain document uri can only be an extra note, and those are never deleted —
            // DeleteNoteUseCase already refuses them, this is the last line behind it.
            require(DocumentsContract.isTreeUri(uri)) { "Refusing to delete a note from outside the notes folder" }
            val deleted = DocumentsContract.deleteDocument(contentResolver, uri)
            Log.d(TAG, "Delete of $fileUri returned $deleted")
            deleted
        }.recoverCatching { error ->
            // The provider throws instead of returning false when the file is already gone —
            // e.g. the list in Settings was loaded before the file was removed some other way.
            // The caller only wants the file not to exist, and it already doesn't: treat this
            // as success instead of surfacing a "delete failed" error for something that isn't
            // there to fail.
            if (error.isMissingFile()) true else throw error
        }
    }

    private fun fileToNoteSummary(docFile: DocumentFile): NoteSummary? {
        return runCatching {
            val name = docFile.name ?: return null
            val title = name.removeSuffix(".md")
            val preview = readPreview(docFile.uri, title)
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

    private fun readPreview(uri: Uri, title: String): String {
        return runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    val lines = mutableListOf<String>()
                    var line: String?
                    while (reader.readLine().also { line = it } != null && lines.size < PREVIEW_SOURCE_LINE_COUNT) {
                        lines.add(line.orEmpty())
                    }
                    previewFormatter.format(lines.joinToString("\n"), title)
                }
            } ?: ""
        }.getOrDefault("")
    }

    companion object {
        private const val TAG = "FileRepository"
        private const val PREVIEW_SOURCE_LINE_COUNT = 36
    }
}

/**
 * Pure so it can be tested without a device. `DocumentsContract.deleteDocument` throws instead
 * of returning false when the target is already gone, wrapped in an `IllegalArgumentException`
 * whose cause is a `FileNotFoundException` — or, on some providers, no wrapping at all. Callers
 * only care whether the file is now absent, and it already is, so this is treated as success
 * rather than a delete failure.
 *
 * Deliberately narrow: only exceptions that specifically say "the file isn't there" are treated
 * as success. Anything else — a revoked permission, a provider crash, disk full — must still
 * surface as a real failure. Silently swallowing those would hide a genuine problem behind a
 * "deleted successfully" that never happened.
 */
internal fun Throwable.isMissingFile(): Boolean =
    this is java.io.FileNotFoundException ||
        cause is java.io.FileNotFoundException ||
        (this is IllegalArgumentException && message?.contains("Missing file") == true)
