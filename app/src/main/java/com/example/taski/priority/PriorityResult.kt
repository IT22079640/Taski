package com.example.taski.priority

data class PriorityResult(
    val score: Int,
    val priorityLevel: PriorityLevel,
    val reasons: List<String>
)
