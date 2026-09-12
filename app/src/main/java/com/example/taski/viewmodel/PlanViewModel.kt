package com.example.taski.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asLiveData
import com.example.taski.TaskiApplication
import com.example.taski.plan.DailyWorkCapacityStore
import com.example.taski.plan.FocusPlan
import com.example.taski.plan.FocusPlanBuilder
import com.example.taski.plan.FocusRecommendation
import com.example.taski.plan.FocusRecommendationBuilder

class PlanViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as TaskiApplication).taskRepository
    private val capacityStore = DailyWorkCapacityStore.from(application)

    private val pendingTasks = repository.observeIncomplete().asLiveData()
    private val dailyMinutes = capacityStore.observeMinutes()

    private val _uiState = MediatorLiveData<UiState>().apply {
        value = UiState.Loading
        addSource(pendingTasks) { rebuild() }
        addSource(dailyMinutes) { rebuild() }
    }
    val uiState: LiveData<UiState> = _uiState

    fun setAvailableMinutes(minutes: Int) {
        capacityStore.setMinutes(minutes)
    }

    fun currentAvailableMinutes(): Int =
        dailyMinutes.value ?: capacityStore.getMinutes()

    fun generatePlan() {
        rebuild()
    }

    fun refreshDisplayPriority() {
        repository.refreshDisplayPriority()
    }

    private fun rebuild() {
        val tasks = pendingTasks.value ?: return
        val available = currentAvailableMinutes()
        val plan = FocusPlanBuilder.build(tasks, available)
        _uiState.value = UiState.Ready(
            plan = plan,
            recommendation = FocusRecommendationBuilder.from(plan)
        )
    }

    sealed class UiState {
        data object Loading : UiState()
        data class Ready(
            val plan: FocusPlan,
            val recommendation: FocusRecommendation
        ) : UiState()
    }
}
