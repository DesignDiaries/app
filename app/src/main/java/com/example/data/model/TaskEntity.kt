package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val dueDate: Long? = null, // Epoch millis at midnight UTC/local
    val dueTimeMinutes: Int? = null, // Minute of day: 0 to 1439 (e.g. 17:30 -> 1050)
    val listId: Long = 1L,
    val tags: String = "", // Comma-separated tags e.g. "work,client"
    val recurrence: Recurrence = Recurrence.NONE,
    val linkedPackageName: String? = null, // Optional linked app (e.g. "com.slack")
    val reminderTime: Long? = null, // Epoch millis when notification should trigger
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
