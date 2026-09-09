package com.example.taski.priority

enum class PriorityLevel {
    HIGH,
    MEDIUM,
    LOW;

    companion object {
        fun fromScore(score: Int): PriorityLevel = when {
            score >= PriorityCalculator.LEVEL_HIGH_MIN -> HIGH
            score >= PriorityCalculator.LEVEL_MEDIUM_MIN -> MEDIUM
            else -> LOW
        }
    }
}
