package com.marfolog.noteswidgetformarkdown.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.usecase.GetNotesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class NotesViewModel(
    private val getNotesUseCase: GetNotesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    fun loadNotes(folderUri: String) {
        viewModelScope.launch {
            _uiState.value = NotesUiState.Loading
            getNotesUseCase(folderUri)
                .catch { e -> _uiState.value = NotesUiState.Error(e.message ?: "Unknown error") }
                .collect { notes ->
                    _uiState.value = if (notes.isEmpty()) {
                        NotesUiState.Empty
                    } else {
                        NotesUiState.Success(notes)
                    }
                }
        }
    }
}

sealed interface NotesUiState {
    data object Loading : NotesUiState
    data object Empty : NotesUiState
    data class Success(val notes: List<NoteSummary>) : NotesUiState
    data class Error(val message: String) : NotesUiState
}
