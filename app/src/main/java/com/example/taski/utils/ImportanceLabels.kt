package com.example.taski.utils

import com.example.taski.data.entity.Importance

object ImportanceLabels {
    fun toLabel(importance: Importance): String = when (importance) {
        Importance.LOW -> "Low"
        Importance.MEDIUM -> "Medium"
        Importance.HIGH -> "High"
    }

    fun fromLabel(label: String): Importance? = when (label.trim().lowercase()) {
        "low" -> Importance.LOW
        "medium" -> Importance.MEDIUM
        "high" -> Importance.HIGH
        else -> null
    }
}
