package com.example.taski.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.taski.TaskiApplication
import com.example.taski.data.entity.Task
import com.example.taski.plan.FocusPlanBuilder
import com.example.taski.progress.DayRange
import com.example.taski.progress.LocalDates
import com.example.taski.progress.StreakCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val taskRepository = (application as TaskiApplication).taskRepository
    private val focusSessionRepository = (application as TaskiApplication).focusSessionRepository
    private val timeZone: TimeZone = TimeZone.getDefault()
    private val todayRange = MutableStateFlow(currentDayRange())

    val uiState: LiveData<HomeUiState> = todayRange.flatMapLatest { range ->
        combine(
            taskRepository.observeIncomplete(),
            taskRepository.observePendingCount(),
            taskRepository.observeCompletedCountBetween(range.startInclusive, range.endExclusive),
            focusSessionRepository.observeSavedDurationBetween(range.startInclusive, range.endExclusive),
            taskRepository.observeCompletedAtTimes()
        ) { incomplete, pending, todayCompleted, todayFocus, completionTimes ->
            val plan = FocusPlanBuilder.build(incomplete, FocusPlanBuilder.DEFAULT_AVAILABLE_MINUTES)
            HomeUiState(
                pendingCount = pending,
                completedToday = todayCompleted,
                todayFocusMillis = todayFocus,
                streakDays = StreakCalculator.currentStreak(
                    completionTimes = completionTimes,
                    nowMillis = range.startInclusive,
                    timeZone = timeZone
                ),
                topTasks = incomplete.take(TOP_TASK_COUNT),
                planTaskCount = plan.taskCount,
                planMinutes = plan.totalPlannedMinutes
            )
        }
    }.asLiveData(viewModelScope.coroutineContext)

    fun refreshDayBounds() {
        todayRange.value = currentDayRange()
    }

    private fun currentDayRange(): DayRange =
        LocalDates.dayRangeContaining(System.currentTimeMillis(), timeZone)

    data class HomeUiState(
        val pendingCount: Int,
        val completedToday: Int,
        val todayFocusMillis: Long,
        val streakDays: Int,
        val topTasks: List<Task>,
        val planTaskCount: Int,
        val planMinutes: Int
    ) {
        val hasPending: Boolean get() = pendingCount > 0
        val hasPlan: Boolean get() = planTaskCount > 0
        val hasProgress: Boolean get() = completedToday > 0 || todayFocusMillis > 0 || streakDays > 0
    }

    private companion object {
        const val TOP_TASK_COUNT = 3
    }
}
