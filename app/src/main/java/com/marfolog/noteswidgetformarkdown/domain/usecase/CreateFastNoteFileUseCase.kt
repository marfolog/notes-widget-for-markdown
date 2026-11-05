package com.marfolog.noteswidgetformarkdown.domain.usecase

import com.marfolog.noteswidgetformarkdown.domain.model.CreatedFile
import com.marfolog.noteswidgetformarkdown.domain.model.FastNoteTarget
import com.marfolog.noteswidgetformarkdown.domain.repository.FileRepository

/** The app's own file for fast notes, created in the notes folder. */
class CreateFastNoteFileUseCase(private val fileRepository: FileRepository) {
    suspend operator fun invoke(folderUri: String, baseName: String): Result<CreatedFile> =
        fileRepository.createFastNoteFile(folderUri, baseName)
}

/** Whether a file is safe to take as the fast note target without telling the user first. */
class CheckFastNoteTargetUseCase(private val fileRepository: FileRepository) {
    suspend operator fun invoke(fileUri: String): Boolean =
        FastNoteTarget.needsWarning(fileRepository.fileSize(fileUri))
}
