package com.example.taski.priority

import com.example.taski.data.entity.Importance
import com.example.taski.data.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PriorityExplainerTest {

    private val calculator = PriorityCalculator()
    private val now = 1_746_489_600_000L

    @Test
    fun highPriority_producesHighExplanationAndStartToday() {
        val task = task(
            deadline = daysFromNow(0),
            importance = Importance.HIGH,
            effort = 120
        )
        val result = calculator.calculate(task, now)
        assertEquals(PriorityLevel.HIGH, result.priorityLevel)

        val explanation = PriorityExplainer.explain(task, result)
        assertTrue(explanation.whyThisTask.startsWith("This task is highly prioritized"))
        assertTrue(explanation.whyThisTask.contains("deadline is today"))
        assertTrue(explanation.whyThisTask.contains("high importance"))
        assertFalse(explanation.whyThisTask.contains("significant effort"))
        assertEquals("Start this task today.", explanation.recommendationTitle)
        assertTrue(explanation.recommendationBody.contains("higher priority"))
    }

    @Test
    fun mediumPriority_producesMediumExplanation() {
        val task = task(
            deadline = daysFromNow(10),
            importance = Importance.MEDIUM,
            effort = 120
        )
        val result = calculator.calculate(task, now)
        assertEquals(PriorityLevel.MEDIUM, result.priorityLevel)

        val explanation = PriorityExplainer.explain(task, result)
        assertTrue(explanation.whyThisTask.contains("scheduled after higher-priority work"))
        assertTrue(explanation.whyThisTask.contains("not immediate") || explanation.whyThisTask.contains("moderate"))
        assertEquals("Schedule this after higher-priority work.", explanation.recommendationTitle)
        assertTrue(explanation.recommendationBody.contains("moderate urgency"))
    }

    @Test
    fun lowPriority_producesLowExplanation() {
        val task = task(
            deadline = daysFromNow(30),
            importance = Importance.LOW,
            effort = 480
        )
        val result = calculator.calculate(task, now)
        assertEquals(PriorityLevel.LOW, result.priorityLevel)

        val explanation = PriorityExplainer.explain(task, result)
        assertTrue(explanation.whyThisTask.contains("deferred"))
        assertTrue(explanation.whyThisTask.contains("not soon"))
        assertTrue(explanation.whyThisTask.contains("lower importance"))
        assertEquals("This can wait.", explanation.recommendationTitle)
        assertTrue(explanation.recommendationBody.contains("more urgent or important"))
    }

    @Test
    fun highPriorityLargeEffort_mentionsSignificantEffort() {
        val task = task(
            deadline = daysFromNow(1),
            importance = Importance.HIGH,
            effort = 240
        )
        val result = calculator.calculate(task, now)
        assertEquals(PriorityLevel.HIGH, result.priorityLevel)

        val explanation = PriorityExplainer.explain(task, result)
        assertTrue(explanation.whyThisTask.contains("tomorrow") || explanation.whyThisTask.contains("deadline"))
        assertTrue(explanation.whyThisTask.contains("high importance"))
        assertTrue(explanation.whyThisTask.contains("significant effort"))
    }

    @Test
    fun completedTask_keepsWhyButMarksComplete() {
        val task = task(
            deadline = daysFromNow(0),
            importance = Importance.HIGH,
            effort = 60,
            completed = true
        )
        val result = calculator.calculate(task, now)
        val explanation = PriorityExplainer.explain(task, result)
        assertTrue(explanation.whyThisTask.startsWith("This task is highly prioritized"))
        assertEquals("Task complete", explanation.recommendationTitle)
        assertTrue(explanation.recommendationBody.contains("already complete"))
    }

    private fun task(
        deadline: Long,
        importance: Importance,
        effort: Int,
        completed: Boolean = false
    ): Task = Task(
        id = 1,
        title = "Sample",
        deadline = deadline,
        importance = importance,
        estimatedEffort = effort,
        category = "Study",
        completed = completed
    )

    private fun daysFromNow(days: Int): Long = now + days * 86_400_000L
}
