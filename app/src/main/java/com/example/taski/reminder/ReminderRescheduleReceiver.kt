package com.example.taski.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.taski.TaskiApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val pending = goAsync()
        val app = context.applicationContext as TaskiApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DeadlineNotificationPoster.ensureChannel(app)
                val tasks = app.taskRepository.getIncomplete()
                app.reminderScheduler.rescheduleAll(tasks)
            } finally {
                pending.finish()
            }
        }
    }
}
