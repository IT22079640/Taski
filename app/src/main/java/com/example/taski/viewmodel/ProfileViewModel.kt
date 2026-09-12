package com.example.taski.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.taski.plan.DailyWorkCapacityStore
import com.example.taski.reminder.NotificationPermissionHelper

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val capacityStore = DailyWorkCapacityStore.from(application)

    val dailyWorkMinutes: LiveData<Int> = capacityStore.observeMinutes()

    fun currentDailyWorkMinutes(): Int = capacityStore.getMinutes()

    fun setDailyWorkMinutes(minutes: Int) {
        capacityStore.setMinutes(minutes)
    }

    fun remindersEnabled(): Boolean =
        NotificationPermissionHelper.canPostNotifications(getApplication())
}
