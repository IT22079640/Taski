package com.example.taski.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.taski.TaskiApplication
import com.example.taski.data.entity.FocusSessionWithTask
import com.example.taski.data.entity.Task
import com.example.taski.progress.DayRange
import com.example.taski.progress.LocalDates
import com.example.taski.progress.ProductivityInsightGenerator
import com.example.taski.progress.ProgressDashboard
import com.example.taski.progress.StreakCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val taskRepository = (application as TaskiApplication).taskRepository
    private val focusSessionRepository = (application as TaskiApplication).focusSessionRepository
    private val timeZone: TimeZone = TimeZone.getDefault()

    private val todayRange = MutableStateFlow(currentDayRange())

    private val todayCompletedTasks = todayRange.flatMapLatest { range ->
        taskRepository.observeCompletedCountBetween(range.startInclusive, range.endExclusive)
    }
    private val todayFocusMillis = todayRange.flatMapLatest { range ->
        focusSessionRepository.observeSavedDurationBetween(range.startInclusive, range.endExclusive)
    }

    val dashboard: LiveData<ProgressDashboard> = combine(
        combine(
            taskRepository.observeCompletedCount(),
            taskRepository.observePendingCount(),
            todayCompletedTasks,
            taskRepository.observeCompletedAtTimes(),
            taskRepository.observeRecentCompleted(RECENT_LIMIT)
        ) { completed, pending, todayCompleted, completionTimes, recentTasks ->
            TaskSlice(completed, pending, todayCompleted, completionTimes, recentTasks)
        },
        combine(
            focusSessionRepository.observeTotalSavedDuration(),
            todayFocusMillis,
            focusSessionRepository.observeCompletedSessionCount(),
            focusSessionRepository.observeRecentWithTask(RECENT_LIMIT)
        ) { totalFocus, todayFocus, completedSessions, recentSessions ->
            FocusSlice(totalFocus, todayFocus, completedSessions, recentSessions)
        },
        combine(todayRange, taskRepository.observeIncomplete()) { range, pending ->
            PendingSlice(range, pending)
        }
    ) { tasks, focus, pending ->
        val streakDays = StreakCalculator.currentStreak(
            completionTimes = tasks.completionTimes,
            nowMillis = pending.range.startInclusive,
            timeZone = timeZone
        )
        val insight = ProductivityInsightGenerator.generate(
            ProductivityInsightGenerator.statsFrom(
                completedTasks = tasks.completed,
                pendingTasks = pending.pendingTasks,
                completedFocusSessions = focus.completedSessions,
                totalFocusMillis = focus.totalFocusMillis,
                recentCompletedTasks = tasks.recentTasks,
                streakDays = streakDays,
                nowMillis = pending.range.startInclusive,
                timeZone = timeZone
            )
        )
        ProgressDashboard(
            completedTasks = tasks.completed,
            pendingTasks = tasks.pending,
            totalFocusMillis = focus.totalFocusMillis,
            completedFocusSessions = focus.completedSessions,
            todayCompletedTasks = tasks.todayCompleted,
            todayFocusMillis = focus.todayFocusMillis,
            streakDays = streakDays,
            recentCompletedTasks = tasks.recentTasks,
            recentFocusSessions = focus.recentSessions,
            insight = insight
        )
    }.asLiveData(viewModelScope.coroutineContext)

    fun refreshDayBounds() {
        taskRepository.refreshDisplayPriority()
        todayRange.value = currentDayRange()
    }

    private fun currentDayRange(): DayRange =
        LocalDates.dayRangeContaining(System.currentTimeMillis(), timeZone)

    private data class TaskSlice(
        val completed: Int,
        val pending: Int,
        val todayCompleted: Int,
        val completionTimes: List<Long>,
        val recentTasks: List<Task>
    )

    private data class FocusSlice(
        val totalFocusMillis: Long,
        val todayFocusMillis: Long,
        val completedSessions: Int,
        val recentSessions: List<FocusSessionWithTask>
    )

    private data class PendingSlice(
        val range: DayRange,
        val pendingTasks: List<Task>
    )

    private companion object {
        const val RECENT_LIMIT = 5
    }
}
