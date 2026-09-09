package com.example.taski.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.taski.TaskiApplication
import com.example.taski.data.entity.Task
import com.example.taski.utils.DateUtils
import com.example.taski.utils.ImportanceLabels
import kotlinx.coroutines.launch

class TaskEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as TaskiApplication).taskRepository

    private val _existingTask = MutableLiveData<Task?>()
    val existingTask: LiveData<Task?> = _existingTask

    private val _formErrors = MutableLiveData<FormErrors>()
    val formErrors: LiveData<FormErrors> = _formErrors

    private val _savedTaskId = MutableLiveData<Long?>()
    val savedTaskId: LiveData<Long?> = _savedTaskId

    fun load(taskId: Long) {
        if (taskId <= 0L) {
            _existingTask.value = null
            return
        }
        viewModelScope.launch {
            _existingTask.value = repository.getById(taskId)
        }
    }

    fun save(
        title: String,
        description: String,
        deadlineMillis: Long?,
        effortText: String,
        importanceLabel: String,
        category: String
    ) {
        val trimmedTitle = title.trim()
        val trimmedCategory = category.trim()
        val importance = ImportanceLabels.fromLabel(importanceLabel)
        val effortHours = effortText.trim().replace(',', '.').toDoubleOrNull()
        val effortMinutes = effortHours?.let { DateUtils.hoursToMinutes(it) }

        val errors = FormErrors(
            title = if (trimmedTitle.isEmpty()) TITLE_REQUIRED else null,
            deadline = if (deadlineMillis == null || deadlineMillis <= 0L) DEADLINE_REQUIRED else null,
            effort = when {
                effortText.trim().isEmpty() -> EFFORT_REQUIRED
                effortHours == null || effortHours <= 0.0 || effortMinutes == null || effortMinutes <= 0 ->
                    EFFORT_POSITIVE
                else -> null
            },
            importance = if (importance == null) IMPORTANCE_REQUIRED else null,
            category = if (trimmedCategory.isEmpty()) CATEGORY_REQUIRED else null
        )

        _formErrors.value = errors
        if (!errors.isValid()) return

        val current = _existingTask.value
        val task = Task(
            id = current?.id ?: 0L,
            title = trimmedTitle,
            description = description.trim(),
            deadline = deadlineMillis!!,
            importance = importance!!,
            estimatedEffort = effortMinutes!!,
            category = trimmedCategory,
            priorityScore = current?.priorityScore ?: 0,
            completed = current?.completed ?: false,
            createdAt = current?.createdAt ?: System.currentTimeMillis(),
            completedAt = current?.completedAt
        )

        viewModelScope.launch {
            val savedId = if (current == null) {
                repository.insert(task)
            } else {
                repository.update(task)
                current.id
            }
            _savedTaskId.value = savedId
        }
    }

    fun onSaveHandled() {
        _savedTaskId.value = null
    }

    data class FormErrors(
        val title: String? = null,
        val deadline: String? = null,
        val effort: String? = null,
        val importance: String? = null,
        val category: String? = null
    ) {
        fun isValid(): Boolean =
            title == null &&
                deadline == null &&
                effort == null &&
                importance == null &&
                category == null
    }

    private companion object {
        const val TITLE_REQUIRED = "Enter a task title"
        const val DEADLINE_REQUIRED = "Choose a valid deadline"
        const val EFFORT_REQUIRED = "Enter estimated effort"
        const val EFFORT_POSITIVE = "Estimated effort must be greater than 0"
        const val IMPORTANCE_REQUIRED = "Select importance"
        const val CATEGORY_REQUIRED = "Enter a category"
    }
}
