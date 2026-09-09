package com.example.taski

import android.app.Application
import com.example.taski.data.database.TaskiDatabase
import com.example.taski.data.repository.FocusSessionRepository
import com.example.taski.data.repository.TaskRepository
import com.example.taski.reminder.AlarmTaskReminderScheduler
import com.example.taski.reminder.DeadlineNotificationPoster
import com.example.taski.reminder.TaskReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TaskiApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: TaskiDatabase by lazy { TaskiDatabase.getInstance(this) }
    val reminderScheduler: TaskReminderScheduler by lazy { AlarmTaskReminderScheduler(this) }
    val taskRepository: TaskRepository by lazy {
        TaskRepository(
            taskDao = database.taskDao(),
            reminderScheduler = reminderScheduler
        )
    }
    val focusSessionRepository: FocusSessionRepository by lazy {
        FocusSessionRepository(database.focusSessionDao())
    }

    override fun onCreate() {
        super.onCreate()
        DeadlineNotificationPoster.ensureChannel(this)
        applicationScope.launch {
            reminderScheduler.rescheduleAll(taskRepository.getIncomplete())
        }
    }
}
