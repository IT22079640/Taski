package com.example.taski.priority

/**
 * Offline suggested steps for large-effort tasks. Display-only — no schema change.
 */
object SmartTaskBreakdown {
    const val MIN_EFFORT_MINUTES = 181

    fun shouldSuggest(estimatedEffortMinutes: Int): Boolean =
        estimatedEffortMinutes >= MIN_EFFORT_MINUTES

    fun stepsFor(category: String): List<String> {
        val key = category.trim().lowercase()
        return when {
            key.contains("study") -> STUDY_STEPS
            key.contains("assignment") || key.contains("project") -> ACADEMIC_STEPS
            key.contains("work") -> WORK_STEPS
            else -> GENERIC_STEPS
        }
    }

    private val ACADEMIC_STEPS = listOf(
        "Define requirements",
        "Plan the work",
        "Complete the main implementation",
        "Test and review",
        "Finalize and submit"
    )

    private val STUDY_STEPS = listOf(
        "Review the material",
        "Identify key topics",
        "Study and practice",
        "Review difficult areas",
        "Complete a final check"
    )

    private val WORK_STEPS = listOf(
        "Clarify the goal",
        "Break the work into parts",
        "Complete the core work",
        "Review quality",
        "Wrap up and share"
    )

    private val GENERIC_STEPS = listOf(
        "Clarify what done looks like",
        "Plan the first step",
        "Complete the main work",
        "Review and improve",
        "Finalize"
    )
}
