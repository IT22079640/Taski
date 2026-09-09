package com.example.taski.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.taski.TaskiApplication
import com.example.taski.data.entity.Task
import com.example.taski.focus.FocusTimerEngine
import com.example.taski.focus.FocusTimerStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FocusViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val taskRepository = (application as TaskiApplication).taskRepository
    private val focusSessionRepository = (application as TaskiApplication).focusSessionRepository

    private val saveMutex = Mutex()
    private var tickerJob: Job? = null
    private var loadedTaskId: Long = 0L
    private var task: Task? = null

    private var totalMillis: Long = savedStateHandle[KEY_TOTAL] ?: FocusTimerEngine.DEFAULT_DURATION_MS
    private var remainingMillis: Long = savedStateHandle[KEY_REMAINING] ?: totalMillis
    private var status: FocusTimerStatus = savedStateHandle.get<String>(KEY_STATUS)
        ?.let { runCatching { FocusTimerStatus.valueOf(it) }.getOrNull() }
        ?: FocusTimerStatus.Idle
    private var sessionStartTime: Long = savedStateHandle[KEY_SESSION_START] ?: 0L
    private var endRealtime: Long = savedStateHandle[KEY_END_REALTIME] ?: 0L
    private var lastRealtime: Long = savedStateHandle[KEY_LAST_REALTIME] ?: 0L
    private var sessionSaved: Boolean = savedStateHandle[KEY_SAVED] ?: false
    private var finishedFully: Boolean = savedStateHandle[KEY_FINISHED_FULLY] ?: false

    private val _uiState = MutableLiveData<UiState>(UiState.Loading)
    val uiState: LiveData<UiState> = _uiState

    init {
        restoreRunningTimer()
        val taskId = savedStateHandle.get<Long>(KEY_TASK_ID) ?: 0L
        if (taskId > 0L) {
            load(taskId)
        } else {
            _uiState.value = UiState.MissingTask
        }
    }

    fun load(taskId: Long) {
        savedStateHandle[KEY_TASK_ID] = taskId
        if (taskId <= 0L) {
            loadedTaskId = 0L
            task = null
            cancelTicker()
            _uiState.value = UiState.MissingTask
            return
        }
        if (loadedTaskId == taskId) {
            when (_uiState.value) {
                is UiState.Ready, is UiState.Loading, is UiState.NotFound -> return
                else -> Unit
            }
        }
        loadedTaskId = taskId
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val loaded = taskRepository.getById(taskId)
            task = loaded
            if (loaded == null) {
                cancelTicker()
                _uiState.value = UiState.NotFound
            } else {
                publishReady(loaded)
            }
        }
    }

    fun setDurationMillis(durationMillis: Long) {
        if (!FocusTimerEngine.canChangeDuration(status)) return
        val valid = FocusTimerEngine.validateDurationMillis(durationMillis)
        if (!valid) return
        totalMillis = durationMillis
        remainingMillis = durationMillis
        persistTimer()
        task?.let { publishReady(it) }
    }

    fun start() {
        val durationValid = FocusTimerEngine.validateDurationMillis(totalMillis)
        if (!FocusTimerEngine.canStart(status, durationValid)) return
        if (status == FocusTimerStatus.Idle) {
            sessionStartTime = System.currentTimeMillis()
            sessionSaved = false
            finishedFully = false
            remainingMillis = totalMillis
        }
        status = FocusTimerStatus.Running
        val now = SystemClock.elapsedRealtime()
        endRealtime = now + remainingMillis
        lastRealtime = now
        persistTimer()
        startTicker()
        task?.let { publishReady(it) }
    }

    fun pause() {
        if (!FocusTimerEngine.canPause(status)) return
        cancelTicker()
        val now = SystemClock.elapsedRealtime()
        remainingMillis = FocusTimerEngine.remainingFromEnd(endRealtime, now)
        lastRealtime = now
        status = FocusTimerStatus.Paused
        persistTimer()
        task?.let { publishReady(it) }
    }

    fun stop() {
        if (!FocusTimerEngine.canStop(status)) return
        cancelTicker()
        val now = SystemClock.elapsedRealtime()
        if (status == FocusTimerStatus.Running) {
            remainingMillis = FocusTimerEngine.remainingFromEnd(endRealtime, now)
        }
        lastRealtime = now
        val duration = FocusTimerEngine.durationToSave(
            completedFully = false,
            totalMillis = totalMillis,
            remainingMillis = remainingMillis
        )
        if (FocusTimerEngine.shouldSave(sessionSaved, duration)) {
            status = FocusTimerStatus.Finished
            finishedFully = false
            persistTimer()
            viewModelScope.launch { saveSession(completedFully = false) }
        } else {
            resetToIdle()
        }
        task?.let { publishReady(it) }
    }

    private fun restoreRunningTimer() {
        if (status != FocusTimerStatus.Running) return
        val now = SystemClock.elapsedRealtime()
        remainingMillis = FocusTimerEngine.remainingAfterRestore(
            wasRunning = true,
            remainingAtSave = remainingMillis,
            endRealtime = endRealtime,
            nowRealtime = now,
            lastRealtime = lastRealtime
        )
        lastRealtime = now
        persistTimer()
        if (remainingMillis <= 0L) {
            viewModelScope.launch { onCountdownFinished() }
        } else {
            endRealtime = now + remainingMillis
            persistTimer()
            startTicker()
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (status == FocusTimerStatus.Running) {
                val now = SystemClock.elapsedRealtime()
                remainingMillis = FocusTimerEngine.remainingFromEnd(endRealtime, now)
                lastRealtime = now
                persistTimer()
                task?.let { publishReady(it) }
                if (remainingMillis <= 0L) {
                    onCountdownFinished()
                    break
                }
                delay(TICK_MS)
            }
        }
    }

    private suspend fun onCountdownFinished() {
        cancelTicker()
        remainingMillis = 0L
        status = FocusTimerStatus.Finished
        finishedFully = true
        persistTimer()
        saveSession(completedFully = true)
        task?.let { publishReady(it) }
    }

    private suspend fun saveSession(completedFully: Boolean) {
        val currentTaskId = when {
            loadedTaskId > 0L -> loadedTaskId
            else -> savedStateHandle.get<Long>(KEY_TASK_ID) ?: 0L
        }
        if (currentTaskId <= 0L) return
        saveMutex.withLock {
            val duration = FocusTimerEngine.durationToSave(
                completedFully = completedFully,
                totalMillis = totalMillis,
                remainingMillis = remainingMillis
            )
            if (!FocusTimerEngine.shouldSave(sessionSaved, duration)) return
            sessionSaved = true
            persistTimer()
            focusSessionRepository.insert(
                FocusTimerEngine.createSession(
                    taskId = currentTaskId,
                    durationMs = duration,
                    startTime = sessionStartTime,
                    completed = completedFully
                )
            )
        }
    }

    private fun resetToIdle() {
        status = FocusTimerStatus.Idle
        remainingMillis = totalMillis
        sessionStartTime = 0L
        sessionSaved = false
        finishedFully = false
        persistTimer()
    }

    private fun cancelTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun persistTimer() {
        savedStateHandle[KEY_TOTAL] = totalMillis
        savedStateHandle[KEY_REMAINING] = remainingMillis
        savedStateHandle[KEY_STATUS] = status.name
        savedStateHandle[KEY_SESSION_START] = sessionStartTime
        savedStateHandle[KEY_END_REALTIME] = endRealtime
        savedStateHandle[KEY_LAST_REALTIME] = lastRealtime
        savedStateHandle[KEY_SAVED] = sessionSaved
        savedStateHandle[KEY_FINISHED_FULLY] = finishedFully
    }

    private fun publishReady(currentTask: Task) {
        val durationValid = FocusTimerEngine.validateDurationMillis(totalMillis)
        _uiState.value = UiState.Ready(
            task = currentTask,
            remainingLabel = FocusTimerEngine.formatCountdown(remainingMillis),
            progress = FocusTimerEngine.progress(totalMillis, remainingMillis),
            status = status,
            finishedFully = finishedFully,
            durationMillis = totalMillis,
            durationValid = durationValid,
            startEnabled = FocusTimerEngine.canStart(status, durationValid),
            pauseEnabled = FocusTimerEngine.canPause(status),
            stopEnabled = FocusTimerEngine.canStop(status),
            durationEnabled = FocusTimerEngine.canChangeDuration(status),
            isResume = status == FocusTimerStatus.Paused,
            showFinished = status == FocusTimerStatus.Finished
        )
    }

    override fun onCleared() {
        cancelTicker()
        super.onCleared()
    }

    sealed class UiState {
        data object Loading : UiState()
        data object MissingTask : UiState()
        data object NotFound : UiState()
        data class Ready(
            val task: Task,
            val remainingLabel: String,
            val progress: Int,
            val status: FocusTimerStatus,
            val finishedFully: Boolean,
            val durationMillis: Long,
            val durationValid: Boolean,
            val startEnabled: Boolean,
            val pauseEnabled: Boolean,
            val stopEnabled: Boolean,
            val durationEnabled: Boolean,
            val isResume: Boolean,
            val showFinished: Boolean
        ) : UiState()
    }

    private companion object {
        const val TICK_MS = 250L
        const val KEY_TASK_ID = "taskId"
        const val KEY_TOTAL = "focus_total_ms"
        const val KEY_REMAINING = "focus_remaining_ms"
        const val KEY_STATUS = "focus_status"
        const val KEY_SESSION_START = "focus_session_start"
        const val KEY_END_REALTIME = "focus_end_realtime"
        const val KEY_LAST_REALTIME = "focus_last_realtime"
        const val KEY_SAVED = "focus_session_saved"
        const val KEY_FINISHED_FULLY = "focus_finished_fully"
    }
}
