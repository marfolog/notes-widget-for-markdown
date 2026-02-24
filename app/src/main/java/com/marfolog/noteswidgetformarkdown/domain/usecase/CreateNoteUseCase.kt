package com.marfolog.noteswidgetformarkdown.domain.usecase

import com.marfolog.noteswidgetformarkdown.domain.repository.FileRepository

class CreateNoteUseCase(private val fileRepository: FileRepository) {

    suspend operator fun invoke(folderUri: String, title: String, content: String): Result<Boolean> {
        return fileRepository.createNote(folderUri, title, content)
    }
}
