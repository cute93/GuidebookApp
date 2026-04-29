package com.example.guidebook.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guidebook.models.AppUser
import com.example.guidebook.models.Problem
import com.example.guidebook.models.UserNote
import com.example.guidebook.repository.GuidebookRepository
import kotlinx.coroutines.launch

class Page1ViewModel(
    private val repo: GuidebookRepository = GuidebookRepository()
) : ViewModel() {

    private val _problems = MutableLiveData<List<Problem>>()
    val problems: LiveData<List<Problem>> = _problems

    private val _currentIndex = MutableLiveData(0)
    val currentIndex: LiveData<Int> = _currentIndex

    private val _notes = MutableLiveData<List<UserNote>>()
    val notes: LiveData<List<UserNote>> = _notes

    private val _currentUser = MutableLiveData<AppUser?>()
    val currentUser: LiveData<AppUser?> = _currentUser

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    val currentProblem: Problem?
        get() = _problems.value?.getOrNull(_currentIndex.value ?: 0)

    fun init(user: AppUser) {
        _currentUser.value = user
        loadProblems()
    }

    private fun loadProblems() {
        viewModelScope.launch {
            repo.getProblems().onSuccess { list ->
                _problems.value = list
                if (list.isNotEmpty()) loadNotes(list[0].id)
            }.onFailure {
                _error.value = it.message
            }
        }
    }

    fun loadNotes(problemId: String) {
        viewModelScope.launch {
            repo.getUserNotes(problemId).onSuccess {
                _notes.value = it
            }.onFailure {
                _error.value = it.message
            }
        }
    }

    fun goToPrev() {
        val idx = (_currentIndex.value ?: 0) - 1
        val list = _problems.value ?: return
        if (idx >= 0) {
            _currentIndex.value = idx
            loadNotes(list[idx].id)
        }
    }

    fun goToNext() {
        val idx = (_currentIndex.value ?: 0) + 1
        val list = _problems.value ?: return
        if (idx < list.size) {
            _currentIndex.value = idx
            loadNotes(list[idx].id)
        }
    }

    fun refreshNotes() {
        currentProblem?.let { loadNotes(it.id) }
    }
}
