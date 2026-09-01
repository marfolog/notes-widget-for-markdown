package com.marfolog.noteswidgetformarkdown.domain.repository

import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun getNotes(folderUri: String): Flow<List<NoteSummary>>
    suspend fun createNote(folderUri: String, title: String, content: String): Result<Boolean>
    suspend fun appendFastNote(fileUri: String, noteText: String): Result<Boolean>

    /** Deletes the note file itself. There is no trash in SAF — this is permanent. */
    suspend fun deleteNote(fileUri: String): Result<Boolean>
}
