package com.example.data.model

enum class Recurrence(val displayName: String) {
    NONE("No repeat"),
    DAILY("Every day"),
    WEEKDAYS("Weekdays (Mon-Fri)"),
    WEEKLY("Every week"),
    MONTHLY("Every month");

    companion object {
        fun fromString(value: String?): Recurrence {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}
