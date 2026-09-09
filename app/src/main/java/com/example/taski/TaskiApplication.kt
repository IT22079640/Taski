package com.example.taski

import android.app.Application
import com.example.taski.data.database.TaskiDatabase
import com.example.taski.data.repository.FocusSessionRepository
import com.example.taski.data.repository.TaskRepository

class TaskiApplication : Application() {
    val database: TaskiDatabase by lazy { TaskiDatabase.getInstance(this) }
    val taskRepository: TaskRepository by lazy { TaskRepository(database.taskDao()) }
    val focusSessionRepository: FocusSessionRepository by lazy {
        FocusSessionRepository(database.focusSessionDao())
    }
}
