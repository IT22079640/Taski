package com.example.taski.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.taski.TaskiApplication
import com.example.taski.data.entity.Task
import kotlinx.coroutines.launch

class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as TaskiApplication).taskRepository

    val tasks: LiveData<List<Task>> = repository.observeAllByPriority().asLiveData()

    fun refreshDisplayPriority() {
        repository.refreshDisplayPriority()
    }

    fun setCompleted(task: Task, completed: Boolean) {
        viewModelScope.launch {
            repository.setCompleted(task.id, completed)
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }
}
