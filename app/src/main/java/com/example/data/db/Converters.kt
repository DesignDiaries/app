package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.Priority
import com.example.data.model.Recurrence

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): Priority = Priority.fromString(value)

    @TypeConverter
    fun fromRecurrence(recurrence: Recurrence): String = recurrence.name

    @TypeConverter
    fun toRecurrence(value: String): Recurrence = Recurrence.fromString(value)
}
