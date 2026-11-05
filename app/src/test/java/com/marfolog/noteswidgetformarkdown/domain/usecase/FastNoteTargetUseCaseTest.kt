package com.marfolog.noteswidgetformarkdown.domain.usecase

import com.marfolog.noteswidgetformarkdown.domain.model.CreatedFile
import com.marfolog.noteswidgetformarkdown.domain.model.FastNoteStyle
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FastNoteTargetUseCaseTest {

    private class FakeRepository(private val size: Long?, private val create: Result<CreatedFile>) : FileRepository {
        var askedBase: String? = null
        override fun getNotes(folderUri: String, extraFileUris: List<String>): Flow<List<NoteSummary>> = emptyFlow()
        override suspend fun getExtraNote(fileUri: String): Result<NoteSummary> = Result.failure(UnsupportedOperationException())
        override suspend fun createNote(folderUri: String, title: String, content: String): Result<String> =
            Result.failure(UnsupportedOperationException())
        override suspend fun createFastNoteFile(folderUri: String, baseName: String): Result<CreatedFile> {
            askedBase = baseName
            return create
        }
        override suspend fun fileSize(fileUri: String): Long? = size
        override suspend fun appendFastNote(fileUri: String, noteText: String, style: FastNoteStyle): Result<Boolean> =
            Result.failure(UnsupportedOperationException())
        override suspend fun deleteNote(fileUri: String): Result<Boolean> = Result.failure(UnsupportedOperationException())
    }

    private val created = Result.success(CreatedFile("content://provider/document/Quick%20notes.md", "Quick notes.md"))

    @Test
    fun `a file with content needs the warning`() = runBlocking {
        assertTrue(CheckFastNoteTargetUseCase(FakeRepository(1_240, created))("content://x"))
    }

    @Test
    fun `an empty file does not`() = runBlocking {
        assertFalse(CheckFastNoteTargetUseCase(FakeRepository(0, created))("content://x"))
    }

    @Test
    fun `a file whose size is unknown needs the warning`() = runBlocking {
        assertTrue(CheckFastNoteTargetUseCase(FakeRepository(null, created))("content://x"))
    }

    @Test
    fun `creating passes the translated base name on and returns the new file`() = runBlocking {
        val repository = FakeRepository(0, created)
        val result = CreateFastNoteFileUseCase(repository)("content://tree", "Rychlé poznámky")
        assertEquals("Rychlé poznámky", repository.askedBase)
        assertEquals("Quick notes.md", result.getOrNull()?.name)
    }

    @Test
    fun `a failed creation is a failure, not a crash`() = runBlocking {
        val repository = FakeRepository(0, Result.failure(SecurityException("lost")))
        assertTrue(CreateFastNoteFileUseCase(repository)("content://tree", "Quick notes").isFailure)
    }
}
