package com.example.taski.data.database

import androidx.room.TypeConverter
import com.example.taski.data.entity.Importance

class Converters {
    @TypeConverter
    fun fromImportance(value: Importance): String = value.name

    @TypeConverter
    fun toImportance(value: String): Importance = Importance.valueOf(value)
}
