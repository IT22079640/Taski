package com.example.taski.priority

import com.example.taski.data.entity.Importance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriorityCalculatorTest {

    private val calculator = PriorityCalculator()
    private val now = 1_746_489_600_000L

    @Test
    fun highImportanceNearDeadline_producesHighScore() {
        val result = calculator.calculate(
            deadlineMillis = daysFromNow(0),
            importance = Importance.HIGH,
            estimatedEffortMinutes = 120,
            nowMillis = now
        )
        assertTrue(result.score >= PriorityCalculator.LEVEL_HIGH_MIN)
        assertEquals(PriorityLevel.HIGH, result.priorityLevel)
        assertTrue(result.reasons.any { it.contains("today", ignoreCase = true) })
        assertTrue(result.reasons.contains("High importance"))
    }

    @Test
    fun lowImportanceDistantDeadline_producesLowerScore() {
        val result = calculator.calculate(
            deadlineMillis = daysFromNow(30),
            importance = Importance.LOW,
            estimatedEffortMinutes = 480,
            nowMillis = now
        )
        assertTrue(result.score < PriorityCalculator.LEVEL_HIGH_MIN)
        assertEquals(PriorityLevel.LOW, result.priorityLevel)
        assertTrue(result.reasons.contains("Low importance"))
        assertTrue(result.reasons.any { it.contains("not soon", ignoreCase = true) })
    }

    @Test
    fun overdueTask_receivesVeryHighUrgency() {
        val overdue = calculator.calculate(
            deadlineMillis = daysFromNow(-2),
            importance = Importance.MEDIUM,
            estimatedEffortMinutes = 120,
            nowMillis = now
        )
        val dueLater = calculator.calculate(
            deadlineMillis = daysFromNow(10),
            importance = Importance.MEDIUM,
            estimatedEffortMinutes = 120,
            nowMillis = now
        )
        assertTrue(overdue.score > dueLater.score)
        assertTrue(overdue.reasons.contains("This task is overdue"))
    }

    @Test
    fun differentEffort_changesScoreWhenOtherFactorsMatch() {
        val shortTask = calculator.calculate(
            deadlineMillis = daysFromNow(5),
            importance = Importance.MEDIUM,
            estimatedEffortMinutes = 90,
            nowMillis = now
        )
        val longTask = calculator.calculate(
            deadlineMillis = daysFromNow(5),
            importance = Importance.MEDIUM,
            estimatedEffortMinutes = 480,
            nowMillis = now
        )
        assertNotEquals(shortTask.score, longTask.score)
    }

    @Test
    fun changingImportance_changesScore() {
        val high = calculator.calculate(daysFromNow(4), Importance.HIGH, 120, now)
        val low = calculator.calculate(daysFromNow(4), Importance.LOW, 120, now)
        assertTrue(high.score > low.score)
    }

    @Test
    fun changingDeadline_changesScore() {
        val soon = calculator.calculate(daysFromNow(1), Importance.MEDIUM, 120, now)
        val later = calculator.calculate(daysFromNow(20), Importance.MEDIUM, 120, now)
        assertTrue(soon.score > later.score)
    }

    @Test
    fun scoreStaysWithinZeroToOneHundred() {
        val samples = listOf(
            calculator.calculate(daysFromNow(-10), Importance.HIGH, 30, now),
            calculator.calculate(daysFromNow(0), Importance.HIGH, 120, now),
            calculator.calculate(daysFromNow(60), Importance.LOW, 600, now)
        )
        samples.forEach { result ->
            assertTrue(result.score in PriorityCalculator.SCORE_MIN..PriorityCalculator.SCORE_MAX)
        }
    }

    @Test
    fun sameInputs_produceTheSameResult() {
        val first = calculator.calculate(daysFromNow(3), Importance.HIGH, 150, now)
        val second = calculator.calculate(daysFromNow(3), Importance.HIGH, 150, now)
        assertEquals(first, second)
    }

    @Test
    fun urgencyRisesAsDeadlineApproaches() {
        val far = calculator.calculate(daysFromNow(10), Importance.MEDIUM, 120, now)
        val near = calculator.calculate(daysFromNow(10), Importance.MEDIUM, 120, now + 9 * 86_400_000L)
        assertTrue(near.urgencyScore > far.urgencyScore)
        assertTrue(near.score > far.score)
        assertEquals(far.importanceScore, near.importanceScore)
        assertEquals(far.effortScore, near.effortScore)
    }

    @Test
    fun weightsRemainFortyFortyTwenty() {
        assertEquals(0.40, PriorityCalculator.WEIGHT_URGENCY, 0.0)
        assertEquals(0.40, PriorityCalculator.WEIGHT_IMPORTANCE, 0.0)
        assertEquals(0.20, PriorityCalculator.WEIGHT_EFFORT, 0.0)
    }

    @Test
    fun levelsUseExistingThresholds() {
        assertEquals(70, PriorityCalculator.LEVEL_HIGH_MIN)
        assertEquals(40, PriorityCalculator.LEVEL_MEDIUM_MIN)
        assertEquals(
            PriorityLevel.HIGH,
            calculator.calculate(daysFromNow(0), Importance.HIGH, 120, now).priorityLevel
        )
        assertEquals(
            PriorityLevel.LOW,
            calculator.calculate(daysFromNow(30), Importance.LOW, 480, now).priorityLevel
        )
    }

    @Test
    fun reasonsMatchActualFactors() {
        val result = calculator.calculate(
            deadlineMillis = daysFromNow(2),
            importance = Importance.HIGH,
            estimatedEffortMinutes = 90,
            nowMillis = now
        )
        assertTrue(result.reasons.contains("Deadline is very close"))
        assertTrue(result.reasons.contains("High importance"))
        assertTrue(result.reasons.contains("Estimated effort is manageable"))
    }

    private fun daysFromNow(days: Int): Long = now + days * 86_400_000L
}
