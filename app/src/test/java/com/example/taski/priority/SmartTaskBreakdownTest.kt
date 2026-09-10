package com.example.taski.priority

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartTaskBreakdownTest {

    @Test
    fun modestEffort_isNotSuggested() {
        assertFalse(SmartTaskBreakdown.shouldSuggest(120))
        assertFalse(SmartTaskBreakdown.shouldSuggest(180))
    }

    @Test
    fun largeEffort_isSuggested() {
        assertTrue(SmartTaskBreakdown.shouldSuggest(181))
        assertTrue(SmartTaskBreakdown.shouldSuggest(240))
    }

    @Test
    fun academicCategory_usesImplementationSteps() {
        val steps = SmartTaskBreakdown.stepsFor("Project")
        assertEquals(5, steps.size)
        assertEquals("Define requirements", steps.first())
        assertEquals("Finalize and submit", steps.last())
    }

    @Test
    fun studyCategory_usesStudySteps() {
        val steps = SmartTaskBreakdown.stepsFor("Study")
        assertTrue(steps.first().contains("Review"))
        assertTrue(steps.any { it.contains("key topics") })
    }
}
