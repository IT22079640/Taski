package com.example.taski.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.taski.MainActivity
import com.example.taski.R
import com.example.taski.data.entity.Task

object DeadlineNotificationPoster {
    const val CHANNEL_ID = "taski_deadline_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun show(
        context: Context,
        task: Task,
        kind: ReminderKind = ReminderKind.DUE
    ): Boolean {
        ensureChannel(context)
        if (!NotificationPermissionHelper.canPostNotifications(context)) return false

        val title = context.getString(titleRes(kind))
        val text = context.getString(bodyRes(kind), task.title)
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            ReminderIds.notificationId(task.id, kind),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(
                ReminderIds.notificationId(task.id, kind),
                notification
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun titleRes(kind: ReminderKind): Int = when (kind) {
        ReminderKind.UPCOMING -> R.string.notification_upcoming_title
        ReminderKind.DUE -> R.string.notification_due_title
        ReminderKind.OVERDUE -> R.string.notification_overdue_title
    }

    private fun bodyRes(kind: ReminderKind): Int = when (kind) {
        ReminderKind.UPCOMING -> R.string.notification_upcoming_body
        ReminderKind.DUE -> R.string.notification_due_body
        ReminderKind.OVERDUE -> R.string.notification_overdue_body
    }
}
