package com.example.taski.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TaskWithFocusSessions(
    @Embedded val task: Task,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val sessions: List<FocusSession>
)
