package com.example.guidebook.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guidebook.repository.GuidebookRepository
import kotlinx.coroutines.launch

class Page2ViewModel : ViewModel() {

    private val repo = GuidebookRepository()

    private val _saveState = MutableLiveData<SaveState>()
    val saveState: LiveData<SaveState> = _saveState

    sealed class SaveState {
        object DraftSaved : SaveState()
        object Uploaded : SaveState()
        data class Error(val message: String) : SaveState()
    }

    fun uploadDrawing(
        problemId: String,
        userId: String,
        userName: String,
        role: String,
        bitmap: Bitmap
    ) {
        viewModelScope.launch {
            _saveState.value = null
            repo.uploadNoteDrawing(problemId, userId, userName, role, bitmap)
                .onSuccess { _saveState.value = SaveState.Uploaded }
                .onFailure { _saveState.value = SaveState.Error(it.message ?: "업로드 실패") }
        }
    }
}
