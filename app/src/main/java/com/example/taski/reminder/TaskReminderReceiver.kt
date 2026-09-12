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
        val kind = parseKind(intent.getStringExtra(AlarmTaskReminderScheduler.EXTRA_KIND))

        val pending = goAsync()
        val app = context.applicationContext as TaskiApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = app.taskRepository.getById(taskId)
                if (task == null || task.completed) return@launch
                val now = System.currentTimeMillis()
                val displayKind = resolvedKind(kind, task.deadline, now) ?: return@launch
                val posted = DeadlineNotificationPoster.show(
                    context.applicationContext,
                    task,
                    displayKind
                )
                if (displayKind == ReminderKind.OVERDUE && posted) {
                    OverdueNoticeStore.from(context).markNotified(task.id)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun parseKind(raw: String?): ReminderKind {
        return raw?.let { runCatching { ReminderKind.valueOf(it) }.getOrNull() } ?: ReminderKind.DUE
    }

    private fun resolvedKind(kind: ReminderKind, deadlineMillis: Long, nowMillis: Long): ReminderKind? {
        return when (kind) {
            ReminderKind.UPCOMING -> when {
                deadlineMillis <= nowMillis -> ReminderKind.OVERDUE
                else -> ReminderKind.UPCOMING
            }
            ReminderKind.DUE -> {
                if (deadlineMillis > nowMillis + EARLY_WINDOW_MS) null else ReminderKind.DUE
            }
            ReminderKind.OVERDUE -> {
                if (deadlineMillis > nowMillis) null else ReminderKind.OVERDUE
            }
        }
    }

    private companion object {
        const val EARLY_WINDOW_MS = 2 * 60 * 1000L
    }
}
