package com.example.taski.priority

/**
 * One-shot copy after an edit. Shown only when the score or level actually changed.
 */
object PriorityChangeExplainer {

    fun explain(
        previousScore: Int,
        newScore: Int,
        deadlineChanged: Boolean,
        importanceChanged: Boolean,
        effortChanged: Boolean
    ): PriorityChangeFeedback {
        val previousLevel = PriorityLevel.fromScore(previousScore)
        val newLevel = PriorityLevel.fromScore(newScore)
        val levelLine = if (previousLevel != newLevel) {
            "${levelLabel(previousLevel)} → ${levelLabel(newLevel)}"
        } else {
            null
        }
        return PriorityChangeFeedback(
            previousScore = previousScore,
            newScore = newScore,
            previousLevel = previousLevel,
            newLevel = newLevel,
            levelLine = levelLine,
            scoreLine = "$previousScore → $newScore",
            explanation = explanation(
                previousScore = previousScore,
                newScore = newScore,
                previousLevel = previousLevel,
                newLevel = newLevel,
                deadlineChanged = deadlineChanged,
                importanceChanged = importanceChanged,
                effortChanged = effortChanged
            )
        )
    }

    private fun explanation(
        previousScore: Int,
        newScore: Int,
        previousLevel: PriorityLevel,
        newLevel: PriorityLevel,
        deadlineChanged: Boolean,
        importanceChanged: Boolean,
        effortChanged: Boolean
    ): String {
        if (previousScore == newScore && previousLevel == newLevel) {
            return ""
        }
        val increased = newScore > previousScore
        val factors = changedFactorPhrases(
            increased = increased,
            deadlineChanged = deadlineChanged,
            importanceChanged = importanceChanged,
            effortChanged = effortChanged
        )
        val joined = if (factors.isEmpty()) {
            "deadline, importance, and effort"
        } else {
            PriorityExplainer.joinAnd(factors)
        }
        return when {
            increased && previousLevel != newLevel ->
                "Your priority increased because $joined."
            increased ->
                "Your priority score increased because $joined."
            else ->
                "The task is now less urgent based on its updated $joined."
        }
    }

    private fun changedFactorPhrases(
        increased: Boolean,
        deadlineChanged: Boolean,
        importanceChanged: Boolean,
        effortChanged: Boolean
    ): List<String> {
        val phrases = mutableListOf<String>()
        if (deadlineChanged) {
            phrases += if (increased) "the deadline is closer" else "deadline"
        }
        if (importanceChanged) {
            phrases += if (increased) "the task is now more important" else "importance"
        }
        if (effortChanged) {
            phrases += if (increased) "estimated effort changed" else "effort"
        }
        return phrases
    }

    private fun levelLabel(level: PriorityLevel): String = when (level) {
        PriorityLevel.HIGH -> "High"
        PriorityLevel.MEDIUM -> "Medium"
        PriorityLevel.LOW -> "Low"
    }
}
