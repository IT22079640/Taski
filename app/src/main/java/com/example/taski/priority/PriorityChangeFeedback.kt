package com.example.taski.priority

data class PriorityChangeFeedback(
    val previousScore: Int,
    val newScore: Int,
    val previousLevel: PriorityLevel,
    val newLevel: PriorityLevel,
    val levelLine: String?,
    val scoreLine: String,
    val explanation: String
) {
    val shouldShow: Boolean
        get() = previousScore != newScore || previousLevel != newLevel

    val levelChanged: Boolean
        get() = previousLevel != newLevel

    val scoreIncreased: Boolean
        get() = newScore > previousScore
}
