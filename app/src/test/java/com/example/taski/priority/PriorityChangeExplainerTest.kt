package com.example.taski.priority

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PriorityChangeExplainerTest {

    @Test
    fun levelAndScoreIncrease_showsLevelLineAndIncreaseCopy() {
        val feedback = PriorityChangeExplainer.explain(
            previousScore = 54,
            newScore = 82,
            deadlineChanged = true,
            importanceChanged = true,
            effortChanged = false
        )
        assertTrue(feedback.shouldShow)
        assertEquals("Medium → High", feedback.levelLine)
        assertEquals("54 → 82", feedback.scoreLine)
        assertTrue(feedback.explanation.contains("priority increased"))
        assertTrue(feedback.explanation.contains("deadline is closer"))
        assertTrue(feedback.explanation.contains("more important"))
    }

    @Test
    fun scoreIncreaseSameLevel_doesNotShowLevelLine() {
        val feedback = PriorityChangeExplainer.explain(
            previousScore = 62,
            newScore = 68,
            deadlineChanged = true,
            importanceChanged = false,
            effortChanged = false
        )
        assertTrue(feedback.shouldShow)
        assertEquals(PriorityLevel.MEDIUM, feedback.previousLevel)
        assertEquals(PriorityLevel.MEDIUM, feedback.newLevel)
        assertNull(feedback.levelLine)
        assertEquals("62 → 68", feedback.scoreLine)
        assertTrue(feedback.explanation.contains("score increased"))
        assertTrue(feedback.explanation.contains("deadline is closer"))
    }

    @Test
    fun scoreDecrease_explainsLowerUrgency() {
        val feedback = PriorityChangeExplainer.explain(
            previousScore = 78,
            newScore = 51,
            deadlineChanged = true,
            importanceChanged = true,
            effortChanged = false
        )
        assertTrue(feedback.shouldShow)
        assertEquals("High → Medium", feedback.levelLine)
        assertTrue(feedback.explanation.contains("less urgent"))
        assertTrue(feedback.explanation.contains("deadline"))
        assertTrue(feedback.explanation.contains("importance"))
    }

    @Test
    fun unchangedScore_isNotShown() {
        val feedback = PriorityChangeExplainer.explain(
            previousScore = 55,
            newScore = 55,
            deadlineChanged = false,
            importanceChanged = false,
            effortChanged = false
        )
        assertFalse(feedback.shouldShow)
        assertEquals("", feedback.explanation)
    }
}
