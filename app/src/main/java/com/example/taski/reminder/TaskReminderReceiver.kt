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
        val kind = intent.getStringExtra(AlarmTaskReminderScheduler.EXTRA_KIND)
            ?.let { runCatching { ReminderKind.valueOf(it) }.getOrNull() }
            ?: return
        if (taskId <= 0L) return

        val pending = goAsync()
        val app = context.applicationContext as TaskiApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = app.taskRepository.getById(taskId)
                if (task == null || task.completed) return@launch
                val now = System.currentTimeMillis()
                if (kind == ReminderKind.OVERDUE && task.deadline > now) return@launch
                if (kind == ReminderKind.UPCOMING && task.deadline <= now) return@launch
                DeadlineNotificationPoster.show(context.applicationContext, task, kind)
            } finally {
                pending.finish()
            }
        }
    }
}
