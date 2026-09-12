package com.example.taski.focus

import com.example.taski.data.entity.FocusSession
import java.util.Locale

/**
 * Pure timer and session rules. Does not start Android timers or write to Room.
 */
object FocusTimerEngine {
    const val PRESET_25_MINUTES = 25
    const val PRESET_50_MINUTES = 50
    const val DEFAULT_DURATION_MS = PRESET_25_MINUTES * 60_000L
    const val PRESET_50_MS = PRESET_50_MINUTES * 60_000L
    const val MIN_DURATION_MINUTES = 1
    const val MAX_DURATION_MINUTES = 240
    const val MIN_DURATION_MS = MIN_DURATION_MINUTES * 60_000L
    const val MAX_DURATION_MS = MAX_DURATION_MINUTES * 60_000L
    const val MIN_SAVE_DURATION_MS = 1_000L
    const val PROGRESS_MAX = 1_000

    fun minutesToMillis(minutes: Int): Long = minutes.toLong() * 60_000L

    fun millisToWholeMinutes(millis: Long): Int =
        (millis.coerceAtLeast(0L) / 60_000L).toInt()

    fun isPresetMillis(millis: Long): Boolean =
        millis == DEFAULT_DURATION_MS || millis == PRESET_50_MS

    fun validateDurationMinutes(minutes: Int): Boolean =
        minutes in MIN_DURATION_MINUTES..MAX_DURATION_MINUTES

    fun validateDurationMillis(millis: Long): Boolean =
        millis in MIN_DURATION_MS..MAX_DURATION_MS

    /**
     * Converts a planner duration into timer millis. Returns null when the
     * session should keep the default 25-minute Pomodoro.
     */
    fun plannedDurationMillis(plannedMinutes: Int): Long? {
        if (plannedMinutes <= 0) return null
        val minutes = plannedMinutes.coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES)
        return minutesToMillis(minutes)
    }

    fun formatCountdown(millis: Long): String {
        val totalSeconds = millis.coerceAtLeast(0L) / 1_000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    fun elapsedMillis(totalMillis: Long, remainingMillis: Long): Long {
        val remaining = remainingMillis.coerceAtLeast(0L)
        return (totalMillis - remaining).coerceAtLeast(0L).coerceAtMost(totalMillis.coerceAtLeast(0L))
    }

    fun durationToSave(
        completedFully: Boolean,
        totalMillis: Long,
        remainingMillis: Long
    ): Long {
        return if (completedFully) {
            totalMillis.coerceAtLeast(0L)
        } else {
            elapsedMillis(totalMillis, remainingMillis)
        }
    }

    fun shouldSave(sessionSaved: Boolean, durationMs: Long): Boolean =
        !sessionSaved && durationMs >= MIN_SAVE_DURATION_MS

    fun remainingFromEnd(endRealtime: Long, nowRealtime: Long): Long =
        (endRealtime - nowRealtime).coerceAtLeast(0L)

    fun remainingAfterRestore(
        wasRunning: Boolean,
        remainingAtSave: Long,
        endRealtime: Long,
        nowRealtime: Long,
        lastRealtime: Long
    ): Long {
        if (!wasRunning) return remainingAtSave.coerceAtLeast(0L)
        if (nowRealtime < lastRealtime) return remainingAtSave.coerceAtLeast(0L)
        return remainingFromEnd(endRealtime, nowRealtime)
    }

    fun progress(totalMillis: Long, remainingMillis: Long): Int {
        if (totalMillis <= 0L) return 0
        return ((remainingMillis.coerceAtLeast(0L) * PROGRESS_MAX) / totalMillis)
            .toInt()
            .coerceIn(0, PROGRESS_MAX)
    }

    fun canChangeDuration(status: FocusTimerStatus): Boolean =
        status == FocusTimerStatus.Idle

    fun canStart(status: FocusTimerStatus, durationValid: Boolean): Boolean =
        durationValid && (status == FocusTimerStatus.Idle || status == FocusTimerStatus.Paused)

    fun canPause(status: FocusTimerStatus): Boolean =
        status == FocusTimerStatus.Running

    fun canStop(status: FocusTimerStatus): Boolean =
        status == FocusTimerStatus.Running || status == FocusTimerStatus.Paused

    fun createSession(
        taskId: Long,
        durationMs: Long,
        startTime: Long,
        completed: Boolean
    ): FocusSession = FocusSession(
        taskId = taskId,
        duration = durationMs,
        startTime = startTime,
        completed = completed
    )
}
