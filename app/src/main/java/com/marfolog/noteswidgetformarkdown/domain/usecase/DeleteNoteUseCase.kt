package com.marfolog.noteswidgetformarkdown.domain.usecase

import com.marfolog.noteswidgetformarkdown.domain.repository.FileRepository

class DeleteNoteUseCase(private val fileRepository: FileRepository) {

    suspend operator fun invoke(fileUri: String): Result<Boolean> {
        return fileRepository.deleteNote(fileUri)
    }
}
