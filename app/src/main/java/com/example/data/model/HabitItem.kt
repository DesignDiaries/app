package com.example.data.model

data class HabitItem(
    val id: String,
    val title: String,
    val emoji: String,
    val isCompleted: Boolean = false,
    val streak: Int = 0
)
