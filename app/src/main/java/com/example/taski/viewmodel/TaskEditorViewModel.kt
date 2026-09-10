package com.example.taski.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.taski.TaskiApplication
import com.example.taski.data.entity.Task
import com.example.taski.priority.PriorityChangeExplainer
import com.example.taski.priority.PriorityChangeFeedback
import com.example.taski.utils.DateUtils
import com.example.taski.utils.ImportanceLabels
import kotlinx.coroutines.launch

class TaskEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as TaskiApplication).taskRepository

    private var editingTaskId: Long = 0L
    private var loadCompleted: Boolean = true
    private var saveInFlight: Boolean = false

    private val _existingTask = MutableLiveData<Task?>()
    val existingTask: LiveData<Task?> = _existingTask

    private val _formErrors = MutableLiveData<FormErrors>()
    val formErrors: LiveData<FormErrors> = _formErrors

    private val _savedTaskId = MutableLiveData<Long?>()
    val savedTaskId: LiveData<Long?> = _savedTaskId

    private val _saveResult = MutableLiveData<SaveResult?>()
    val saveResult: LiveData<SaveResult?> = _saveResult

    private val _saveEnabled = MutableLiveData(true)
    val saveEnabled: LiveData<Boolean> = _saveEnabled

    private val _editMissing = MutableLiveData(false)
    val editMissing: LiveData<Boolean> = _editMissing

    fun load(taskId: Long) {
        editingTaskId = taskId
        _editMissing.value = false
        if (taskId <= 0L) {
            loadCompleted = true
            _existingTask.value = null
            _saveEnabled.value = true
            return
        }
        loadCompleted = false
        _saveEnabled.value = false
        viewModelScope.launch {
            val loaded = repository.getById(taskId)
            _existingTask.value = loaded
            loadCompleted = true
            if (loaded == null) {
                _editMissing.value = true
                _saveEnabled.value = false
            } else {
                _saveEnabled.value = true
            }
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
        if (saveInFlight) return
        if (editingTaskId > 0L && !loadCompleted) return
        if (editingTaskId > 0L && _existingTask.value == null) return

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
            id = if (editingTaskId > 0L) editingTaskId else 0L,
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

        saveInFlight = true
        _saveEnabled.value = false
        viewModelScope.launch {
            try {
                val previous = current
                val savedId = if (editingTaskId > 0L) {
                    repository.update(task)
                    editingTaskId
                } else {
                    repository.insert(task)
                }
                val saved = repository.getById(savedId)
                val feedback = if (previous != null && saved != null) {
                    PriorityChangeExplainer.explain(
                        previousScore = previous.priorityScore,
                        newScore = saved.priorityScore,
                        deadlineChanged = previous.deadline != saved.deadline,
                        importanceChanged = previous.importance != saved.importance,
                        effortChanged = previous.estimatedEffort != saved.estimatedEffort
                    )
                } else {
                    null
                }
                _saveResult.value = SaveResult(
                    savedId = savedId,
                    isEdit = previous != null,
                    priorityChange = feedback
                )
                _savedTaskId.value = savedId
            } finally {
                saveInFlight = false
                if (_savedTaskId.value == null) {
                    _saveEnabled.value = true
                }
            }
        }
    }

    fun onSaveHandled() {
        _savedTaskId.value = null
        _saveResult.value = null
    }

    data class SaveResult(
        val savedId: Long,
        val isEdit: Boolean,
        val priorityChange: PriorityChangeFeedback?
    )

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
