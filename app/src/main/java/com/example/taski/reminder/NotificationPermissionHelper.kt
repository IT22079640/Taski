package com.example.taski.reminder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.taski.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object NotificationPermissionHelper {
    private const val PREFS = "taski_prefs"
    private const val KEY_PROMPTED = "notification_permission_prompted"

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun maybePrompt(
        activity: AppCompatActivity,
        launcher: ActivityResultLauncher<String>
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (hasNotificationPermission(activity)) return
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prompted = prefs.getBoolean(KEY_PROMPTED, false)
        val showRationale = activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        if (prompted && !showRationale) return

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.notification_permission_title)
            .setMessage(R.string.notification_permission_body)
            .setPositiveButton(R.string.notification_permission_allow) { _, _ ->
                prefs.edit().putBoolean(KEY_PROMPTED, true).apply()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton(R.string.notification_permission_not_now) { _, _ ->
                prefs.edit().putBoolean(KEY_PROMPTED, true).apply()
            }
            .show()
    }
}
