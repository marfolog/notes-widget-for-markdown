package com.marfolog.noteswidgetformarkdown.domain.usecase

import com.marfolog.noteswidgetformarkdown.domain.model.CreatedFile
import com.marfolog.noteswidgetformarkdown.domain.model.FastNoteStyle
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Removing an extra note from the widget must never delete the file. That promise is written in
 * the dialog, so it is checked here: the extra path must not even reach the repository.
 */
class DeleteNoteUseCaseTest {

    private class RecordingRepository : FileRepository {
        val deleted = mutableListOf<String>()
        override fun getNotes(folderUri: String, extraFileUris: List<String>): Flow<List<NoteSummary>> = emptyFlow()
        override suspend fun getExtraNote(fileUri: String): Result<NoteSummary> = Result.failure(UnsupportedOperationException())
        override suspend fun createNote(folderUri: String, title: String, content: String): Result<String> =
            Result.failure(UnsupportedOperationException())
        override suspend fun createFastNoteFile(folderUri: String, baseName: String): Result<CreatedFile> =
            Result.failure(UnsupportedOperationException())
        override suspend fun fileSize(fileUri: String): Long? = null
        override suspend fun appendFastNote(fileUri: String, noteText: String, style: FastNoteStyle): Result<Boolean> =
            Result.failure(UnsupportedOperationException())
        override suspend fun deleteNote(fileUri: String): Result<Boolean> {
            deleted += fileUri
            return Result.success(true)
        }
    }

    @Test
    fun `an extra note is refused and never reaches deleteNote`() = runBlocking {
        val repository = RecordingRepository()
        val result = DeleteNoteUseCase(repository)(note("content://provider/document/Gifts.md", isExtra = true))
        assertTrue(result.isFailure)
        assertEquals(emptyList<String>(), repository.deleted)
    }

    @Test
    fun `a folder note is still deleted`() = runBlocking {
        val repository = RecordingRepository()
        val uri = "content://provider/tree/MyNotes/document/MyNotes%2FProjects.md"
        val result = DeleteNoteUseCase(repository)(note(uri))
        assertEquals(true, result.getOrNull())
        assertEquals(listOf(uri), repository.deleted)
    }

    private fun note(uri: String, isExtra: Boolean = false) = NoteSummary(
        id = uri,
        title = "",
        preview = "",
        lastModified = 0L,
        fileUri = uri,
        fileName = "",
        isExtra = isExtra
    )
}
