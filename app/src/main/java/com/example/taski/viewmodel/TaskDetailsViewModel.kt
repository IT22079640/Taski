package com.example.taski.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.taski.TaskiApplication
import com.example.taski.data.entity.FocusSession
import com.example.taski.data.entity.Task
import com.example.taski.priority.PriorityResult
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TaskDetailsViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val taskRepository = (application as TaskiApplication).taskRepository
    private val focusSessionRepository = (application as TaskiApplication).focusSessionRepository

    val taskId: Long = savedStateHandle.get<Long>(KEY_TASK_ID) ?: 0L

    private val _deleted = MutableLiveData(false)
    val deleted: LiveData<Boolean> = _deleted

    private val _uiState = MutableLiveData<UiState>(
        if (taskId <= 0L) UiState.NotFound else UiState.Loading
    )
    val uiState: LiveData<UiState> = _uiState

    init {
        if (taskId > 0L) {
            viewModelScope.launch {
                combine(
                    taskRepository.observeById(taskId),
                    focusSessionRepository.observeByTaskId(taskId)
                ) { task, sessions ->
                    if (task == null) {
                        UiState.NotFound
                    } else {
                        UiState.Ready(
                            task = task,
                            priority = taskRepository.explainPriority(task),
                            sessions = sessions,
                            sessionCount = sessions.size,
                            totalFocusMillis = sessions.sumOf { it.duration }
                        )
                    }
                }.collect { state ->
                    if (_deleted.value != true) {
                        _uiState.value = state
                    }
                }
            }
        }
    }

    fun setCompleted(completed: Boolean) {
        if (taskId <= 0L) return
        viewModelScope.launch {
            taskRepository.setCompleted(taskId, completed)
        }
    }

    fun delete() {
        if (taskId <= 0L || _deleted.value == true) return
        viewModelScope.launch {
            val task = taskRepository.getById(taskId) ?: return@launch
            taskRepository.delete(task)
            _deleted.value = true
        }
    }

    sealed class UiState {
        data object Loading : UiState()
        data object NotFound : UiState()
        data class Ready(
            val task: Task,
            val priority: PriorityResult,
            val sessions: List<FocusSession>,
            val sessionCount: Int,
            val totalFocusMillis: Long
        ) : UiState()
    }

    private companion object {
        const val KEY_TASK_ID = "taskId"
    }
}
