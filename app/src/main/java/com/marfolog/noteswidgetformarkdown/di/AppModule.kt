package com.marfolog.noteswidgetformarkdown.di

import com.marfolog.noteswidgetformarkdown.data.repository.FileRepositoryImpl
import com.marfolog.noteswidgetformarkdown.domain.repository.FileRepository
import com.marfolog.noteswidgetformarkdown.domain.usecase.AppendFastNoteUseCase
import com.marfolog.noteswidgetformarkdown.domain.usecase.CreateNoteUseCase
import com.marfolog.noteswidgetformarkdown.domain.usecase.GetNotesUseCase
import com.marfolog.noteswidgetformarkdown.presentation.viewmodel.NotesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<FileRepository> { FileRepositoryImpl(context = get()) }
    factory { GetNotesUseCase(fileRepository = get()) }
    factory { CreateNoteUseCase(fileRepository = get()) }
    factory { AppendFastNoteUseCase(fileRepository = get()) }
    viewModel { NotesViewModel(getNotesUseCase = get()) }
}
