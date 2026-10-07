package com.example.data.model

import java.util.UUID

/**
 * Represents a custom app folder / grouping created by the user or initialized
 * from smart categorization presets (e.g., Productivity, Social, Work, Finance, Reading).
 */
data class CustomAppFolder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconName: String = "FOLDER", // FOLDER, WORK, SOCIAL, FINANCE, READING, STAR, BOLT
    val colorHex: String = "#6366F1",
    val packageNames: List<String> = emptyList(),
    val isPinnedToDock: Boolean = false,
    val isPreset: Boolean = false
)
