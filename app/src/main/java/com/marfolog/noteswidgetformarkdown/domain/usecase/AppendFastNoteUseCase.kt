package com.marfolog.noteswidgetformarkdown.domain.usecase

import com.marfolog.noteswidgetformarkdown.domain.repository.FileRepository

class AppendFastNoteUseCase(private val fileRepository: FileRepository) {

    suspend operator fun invoke(fileUri: String, noteText: String): Result<Boolean> {
        return fileRepository.appendFastNote(fileUri, noteText)
    }
}
