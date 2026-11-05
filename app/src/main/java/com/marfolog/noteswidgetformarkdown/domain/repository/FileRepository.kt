package com.marfolog.noteswidgetformarkdown.domain.repository

import com.marfolog.noteswidgetformarkdown.domain.model.CreatedFile
import com.marfolog.noteswidgetformarkdown.domain.model.FastNoteStyle
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    /**
     * The folder's notes plus the [extraFileUris] added on their own. An extra note that can no
     * longer be read is skipped, and one that also lives in the folder is listed once.
     */
    fun getNotes(folderUri: String, extraFileUris: List<String> = emptyList()): Flow<List<NoteSummary>>

    /** A single extra note. Fails when the file is gone or access to it was revoked. */
    suspend fun getExtraNote(fileUri: String): Result<NoteSummary>

    /** Returns the uri of the file the provider actually created, which is not always the
     *  name that was asked for — SAF providers pick their own name on a collision. */
    suspend fun createNote(folderUri: String, title: String, content: String): Result<String>

    /**
     * Creates the app's own file for fast notes in the folder: [baseName].md, or the first free
     * "[baseName] 2.md", "[baseName] 3.md"… when the name is taken.
     */
    suspend fun createFastNoteFile(folderUri: String, baseName: String): Result<CreatedFile>

    /** Size of the file in bytes; null when the provider does not say or the file cannot be reached. */
    suspend fun fileSize(fileUri: String): Long?

    suspend fun appendFastNote(fileUri: String, noteText: String, style: FastNoteStyle): Result<Boolean>

    /** Deletes the note file itself. There is no trash in SAF — this is permanent. */
    suspend fun deleteNote(fileUri: String): Result<Boolean>
}
