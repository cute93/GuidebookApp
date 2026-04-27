package com.example.guidebook.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guidebook.models.Problem
import com.example.guidebook.repository.GuidebookRepository
import kotlinx.coroutines.launch

class Page3ViewModel : ViewModel() {

    private val repo = GuidebookRepository()

    private val _uploadState = MutableLiveData<UploadState?>()
    val uploadState: LiveData<UploadState?> = _uploadState

    sealed class UploadState {
        data class Success(val problem: Problem) : UploadState()
        data class Error(val message: String) : UploadState()
        object Loading : UploadState()
    }

    fun uploadProblem(title: String, subject: String, imageUri: Uri, context: Context) {
        if (title.isBlank()) { _uploadState.value = UploadState.Error("제목을 입력해주세요."); return }
        _uploadState.value = UploadState.Loading
        viewModelScope.launch {
            repo.uploadProblem(title, subject, imageUri, context)
                .onSuccess { _uploadState.value = UploadState.Success(it) }
                .onFailure { _uploadState.value = UploadState.Error(it.message ?: "업로드 실패") }
        }
    }
}
