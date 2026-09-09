package com.example.taski.data.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class FocusSessionWithTask(
    @Embedded val session: FocusSession,
    @ColumnInfo(name = "taskTitle") val taskTitle: String
)
