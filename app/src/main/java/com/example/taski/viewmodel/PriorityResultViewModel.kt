package com.example.taski.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.taski.TaskiApplication
import com.example.taski.data.entity.Task
import com.example.taski.priority.PriorityExplanation
import com.example.taski.priority.PriorityExplainer
import com.example.taski.priority.PriorityResult
import kotlinx.coroutines.launch

class PriorityResultViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as TaskiApplication).taskRepository

    private val _uiState = MutableLiveData<UiState>(UiState.Loading)
    val uiState: LiveData<UiState> = _uiState

    fun load(taskId: Long) {
        if (taskId <= 0L) {
            _uiState.value = UiState.NotFound
            return
        }
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val task = repository.getById(taskId)
            _uiState.value = if (task == null) {
                UiState.NotFound
            } else {
                val priority = repository.explainPriority(task)
                UiState.Ready(
                    task = task,
                    priority = priority,
                    explanation = PriorityExplainer.explain(task, priority)
                )
            }
        }
    }

    sealed class UiState {
        data object Loading : UiState()
        data object NotFound : UiState()
        data class Ready(
            val task: Task,
            val priority: PriorityResult,
            val explanation: PriorityExplanation
        ) : UiState()
    }
}
