package com.example.data.model

enum class FocusMode(
    val title: String,
    val description: String,
    val allowedTag: String?,
    val colorHex: String = "#6366F1"
) {
    OFF("Standard", "All apps & tasks visible", null, "#6366F1"),
    DEEP_WORK("Deep Work", "Highest priority & urgent tasks only", null, "#EF4444"),
    WORK("Work", "Work projects & quiet social notifications", "work", "#3B82F6"),
    STUDY("Study", "Deep learning & deadlines, no distractions", "study", "#8B5CF6"),
    PERSONAL("Personal", "Errands, household & personal wellbeing", "personal", "#10B981"),
    ZEN("Zen", "Minimalist calm mode, zero distractions", null, "#14B8A6")
}

data class LauncherSettings(
    val themePalette: String = "OBSIDIAN", // OBSIDIAN, SLATE, INDIGO, MINT, SUNSET, AMOLED
    val wallpaperPreset: String = "NEBULA", // NEBULA, AURORA, EMBER, SLATE, PITCH_BLACK, CUSTOM
    val customWallpaperUri: String? = null,
    val wallpaperScrimAlpha: Float = 0.45f,
    val widgetGlassOpacity: Float = 0.85f,
    val widgetDensity: String = "COMFORTABLE", // COMPACT, COMFORTABLE
    val gridColumns: Int = 4,
    val gridRows: Int = 5,
    val iconSizeDp: Int = 56,
    val showLabels: Boolean = true,
    val lockLayout: Boolean = false,
    val activeFocusMode: FocusMode = FocusMode.OFF,
    val zenGlanceMode: Boolean = false,
    val taskWidgetCompact: Boolean = false,
    val autoRescheduleOverdue: Boolean = true,
    val defaultSearchEngine: String = "Google"
)
