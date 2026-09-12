package com.example.taski.plan

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Local daily work capacity in minutes. Uses the existing Taski SharedPreferences file.
 */
class DailyWorkCapacityStore(private val prefs: SharedPreferences) {

    fun getMinutes(): Int = sanitize(prefs.getInt(KEY_MINUTES, DEFAULT_MINUTES))

    fun setMinutes(minutes: Int) {
        val sanitized = sanitize(minutes)
        if (prefs.getInt(KEY_MINUTES, DEFAULT_MINUTES) == sanitized && prefs.contains(KEY_MINUTES)) {
            return
        }
        prefs.edit().putInt(KEY_MINUTES, sanitized).apply()
    }

    fun observeMinutes(): LiveData<Int> = object : LiveData<Int>(getMinutes()) {
        private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == KEY_MINUTES) {
                postValue(getMinutes())
            }
        }

        override fun onActive() {
            value = getMinutes()
            prefs.registerOnSharedPreferenceChangeListener(listener)
        }

        override fun onInactive() {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    fun minutesFlow(): Flow<Int> = callbackFlow {
        trySend(getMinutes())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == KEY_MINUTES) {
                trySend(getMinutes())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    companion object {
        const val PREFS_NAME = "taski_prefs"
        const val KEY_MINUTES = "daily_work_minutes"
        const val DEFAULT_MINUTES = FocusPlanBuilder.DEFAULT_AVAILABLE_MINUTES
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = FocusPlanBuilder.MAX_AVAILABLE_MINUTES

        fun from(context: Context): DailyWorkCapacityStore =
            DailyWorkCapacityStore(
                context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            )

        fun sanitize(minutes: Int): Int = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)
    }
}
