package com.example.taski.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.taski.TaskiApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != AlarmTaskReminderScheduler.ACTION_DEADLINE_REMINDER) return
        val taskId = intent.getLongExtra(AlarmTaskReminderScheduler.EXTRA_TASK_ID, 0L)
        if (taskId <= 0L) return

        val pending = goAsync()
        val app = context.applicationContext as TaskiApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = app.taskRepository.getById(taskId)
                if (task == null || task.completed) return@launch
                val now = System.currentTimeMillis()
                if (task.deadline > now + EARLY_WINDOW_MS) return@launch
                DeadlineNotificationPoster.show(context.applicationContext, task)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val EARLY_WINDOW_MS = 2 * 60 * 1000L
    }
}
