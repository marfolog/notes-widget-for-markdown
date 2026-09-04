package com.marfolog.noteswidgetformarkdown.domain.usecase

import com.marfolog.noteswidgetformarkdown.domain.repository.FileRepository

class CreateNoteUseCase(private val fileRepository: FileRepository) {

    /** Returns the uri of the file that was actually created. */
    suspend operator fun invoke(folderUri: String, title: String, content: String): Result<String> {
        return fileRepository.createNote(folderUri, title, content)
    }
}
