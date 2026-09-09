package com.example.taski.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.taski.data.entity.Task

class AlarmTaskReminderScheduler(
    context: Context
) : TaskReminderScheduler {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(task: Task) {
        cancel(task.id)
        if (task.id <= 0L || task.completed) return
        val plan = ReminderPlanner.plan(
            deadlineMillis = task.deadline,
            nowMillis = System.currentTimeMillis(),
            completed = false
        )
        plan.upcomingAt?.let { setAlarm(it, task.id, ReminderKind.UPCOMING) }
        plan.overdueAt?.let { setAlarm(it, task.id, ReminderKind.OVERDUE) }
    }

    override fun cancel(taskId: Long) {
        if (taskId <= 0L) return
        ReminderKind.entries.forEach { kind ->
            alarmManager.cancel(pendingIntent(taskId, kind))
        }
    }

    override fun rescheduleAll(tasks: List<Task>) {
        tasks.forEach { schedule(it) }
    }

    private fun setAlarm(triggerAtMillis: Long, taskId: Long, kind: ReminderKind) {
        val pendingIntent = pendingIntent(taskId, kind)
        try {
            if (canUseExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun canUseExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun pendingIntent(taskId: Long, kind: ReminderKind): PendingIntent {
        val intent = Intent(appContext, TaskReminderReceiver::class.java).apply {
            action = ACTION_DEADLINE_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_KIND, kind.name)
        }
        return PendingIntent.getBroadcast(
            appContext,
            ReminderIds.notificationId(taskId, kind),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_DEADLINE_REMINDER = "com.example.taski.action.DEADLINE_REMINDER"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_KIND = "reminder_kind"
    }
}
