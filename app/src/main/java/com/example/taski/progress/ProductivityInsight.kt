package com.example.taski.progress

data class ProductivityInsight(
    val title: String,
    val body: String,
    val kind: Kind
) {
    enum class Kind {
        EMPTY,
        CAUGHT_UP,
        URGENT,
        HIGH_EFFORT,
        SHORT_FOCUS,
        STRONG_COMPLETION,
        CONSISTENCY,
        GENERAL
    }
}

data class InsightStats(
    val completedTasks: Int,
    val pendingTasks: Int,
    val highPriorityPendingCount: Int,
    val overduePendingCount: Int,
    val highEffortPendingCount: Int,
    val completedFocusSessions: Int,
    val averageSessionMinutes: Int,
    val recentCompletedCount: Int,
    val recentHighPriorityCompletedCount: Int,
    val streakDays: Int,
    val totalFocusMillis: Long = 0
)
