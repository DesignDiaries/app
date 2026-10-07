package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class Priority(val title: String, val level: Int, val color: Color) {
    LOW("Low", 0, Color(0xFF10B981)),
    MEDIUM("Medium", 1, Color(0xFF3B82F6)),
    HIGH("High", 2, Color(0xFFF59E0B)),
    URGENT("Urgent", 3, Color(0xFFEF4444));

    companion object {
        fun fromString(value: String?): Priority {
            return when (value?.uppercase()?.trim()) {
                "URGENT", "!URGENT", "P0" -> URGENT
                "HIGH", "!HIGH", "P1" -> HIGH
                "MED", "MEDIUM", "!MED", "P2" -> MEDIUM
                "LOW", "!LOW", "P3" -> LOW
                else -> MEDIUM
            }
        }
    }
}
