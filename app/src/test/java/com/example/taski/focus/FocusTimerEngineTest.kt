package com.example.taski.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTimerEngineTest {

    @Test
    fun defaultDuration_isTwentyFiveMinutes() {
        assertEquals(25 * 60_000L, FocusTimerEngine.DEFAULT_DURATION_MS)
    }

    @Test
    fun formatCountdown_padsSeconds() {
        assertEquals("25:00", FocusTimerEngine.formatCountdown(FocusTimerEngine.DEFAULT_DURATION_MS))
        assertEquals("0:05", FocusTimerEngine.formatCountdown(5_000L))
        assertEquals("0:00", FocusTimerEngine.formatCountdown(0L))
        assertEquals("1:01", FocusTimerEngine.formatCountdown(61_000L))
    }

    @Test
    fun validateDuration_rejectsOutOfRange() {
        assertFalse(FocusTimerEngine.validateDurationMinutes(0))
        assertFalse(FocusTimerEngine.validateDurationMinutes(-5))
        assertFalse(FocusTimerEngine.validateDurationMinutes(181))
        assertTrue(FocusTimerEngine.validateDurationMinutes(1))
        assertTrue(FocusTimerEngine.validateDurationMinutes(25))
        assertTrue(FocusTimerEngine.validateDurationMinutes(180))
        assertFalse(FocusTimerEngine.validateDurationMillis(0L))
        assertTrue(FocusTimerEngine.validateDurationMillis(FocusTimerEngine.PRESET_50_MS))
    }

    @Test
    fun elapsedMillis_tracksTimeUsed() {
        assertEquals(0L, FocusTimerEngine.elapsedMillis(1_500_000L, 1_500_000L))
        assertEquals(60_000L, FocusTimerEngine.elapsedMillis(1_500_000L, 1_440_000L))
        assertEquals(1_500_000L, FocusTimerEngine.elapsedMillis(1_500_000L, 0L))
        assertEquals(0L, FocusTimerEngine.elapsedMillis(1_500_000L, 2_000_000L))
    }

    @Test
    fun durationToSave_usesFullTimeWhenCompleted() {
        val total = FocusTimerEngine.DEFAULT_DURATION_MS
        assertEquals(total, FocusTimerEngine.durationToSave(true, total, 12_000L))
        assertEquals(30_000L, FocusTimerEngine.durationToSave(false, total, total - 30_000L))
    }

    @Test
    fun shouldSave_preventsDuplicatesAndTinySessions() {
        assertFalse(FocusTimerEngine.shouldSave(sessionSaved = true, durationMs = 60_000L))
        assertFalse(FocusTimerEngine.shouldSave(sessionSaved = false, durationMs = 500L))
        assertTrue(FocusTimerEngine.shouldSave(sessionSaved = false, durationMs = 1_000L))
    }

    @Test
    fun remainingAfterRestore_continuesRunningSession() {
        val remaining = FocusTimerEngine.remainingAfterRestore(
            wasRunning = true,
            remainingAtSave = 60_000L,
            endRealtime = 80_000L,
            nowRealtime = 50_000L,
            lastRealtime = 20_000L
        )
        assertEquals(30_000L, remaining)
    }

    @Test
    fun remainingAfterRestore_keepsPausedTime() {
        val remaining = FocusTimerEngine.remainingAfterRestore(
            wasRunning = false,
            remainingAtSave = 40_000L,
            endRealtime = 90_000L,
            nowRealtime = 70_000L,
            lastRealtime = 50_000L
        )
        assertEquals(40_000L, remaining)
    }

    @Test
    fun remainingAfterRestore_handlesRealtimeReset() {
        val remaining = FocusTimerEngine.remainingAfterRestore(
            wasRunning = true,
            remainingAtSave = 55_000L,
            endRealtime = 9_000_000L,
            nowRealtime = 1_000L,
            lastRealtime = 8_900_000L
        )
        assertEquals(55_000L, remaining)
    }

    @Test
    fun buttonRules_matchTimerStatus() {
        assertTrue(FocusTimerEngine.canStart(FocusTimerStatus.Idle, durationValid = true))
        assertTrue(FocusTimerEngine.canStart(FocusTimerStatus.Paused, durationValid = true))
        assertFalse(FocusTimerEngine.canStart(FocusTimerStatus.Running, durationValid = true))
        assertFalse(FocusTimerEngine.canStart(FocusTimerStatus.Idle, durationValid = false))
        assertTrue(FocusTimerEngine.canPause(FocusTimerStatus.Running))
        assertFalse(FocusTimerEngine.canPause(FocusTimerStatus.Paused))
        assertTrue(FocusTimerEngine.canStop(FocusTimerStatus.Paused))
        assertFalse(FocusTimerEngine.canStop(FocusTimerStatus.Idle))
        assertTrue(FocusTimerEngine.canChangeDuration(FocusTimerStatus.Idle))
        assertFalse(FocusTimerEngine.canChangeDuration(FocusTimerStatus.Running))
    }

    @Test
    fun createSession_mapsFieldsWithoutDuplicatingLogic() {
        val session = FocusTimerEngine.createSession(
            taskId = 42L,
            durationMs = 120_000L,
            startTime = 1_700_000_000_000L,
            completed = true
        )
        assertEquals(42L, session.taskId)
        assertEquals(120_000L, session.duration)
        assertEquals(1_700_000_000_000L, session.startTime)
        assertTrue(session.completed)
        assertEquals(0L, session.id)
    }

    @Test
    fun minutesConversion_isDeterministic() {
        assertEquals(15 * 60_000L, FocusTimerEngine.minutesToMillis(15))
        assertEquals(15, FocusTimerEngine.millisToWholeMinutes(15 * 60_000L + 12_000L))
        assertTrue(FocusTimerEngine.isPresetMillis(FocusTimerEngine.DEFAULT_DURATION_MS))
        assertFalse(FocusTimerEngine.isPresetMillis(15 * 60_000L))
    }

    @Test
    fun progress_shrinksWithRemainingTime() {
        assertEquals(FocusTimerEngine.PROGRESS_MAX, FocusTimerEngine.progress(100_000L, 100_000L))
        assertEquals(500, FocusTimerEngine.progress(100_000L, 50_000L))
        assertEquals(0, FocusTimerEngine.progress(100_000L, 0L))
        assertEquals(0, FocusTimerEngine.progress(0L, 0L))
    }
}
