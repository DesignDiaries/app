package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Default Indigo Theme
val IndigoPrimary = Color(0xFF6366F1)
val IndigoSecondary = Color(0xFF818CF8)
val IndigoBackground = Color(0xFF0F172A)
val IndigoSurface = Color(0xFF1E293B)
val IndigoSurfaceVariant = Color(0xFF334155)

// Obsidian Theme
val ObsidianBackground = Color(0xFF090D16)
val ObsidianSurface = Color(0xFF131B2E)
val ObsidianSurfaceVariant = Color(0xFF1E293B)
val ObsidianPrimary = Color(0xFF38BDF8)
val ObsidianAccent = Color(0xFF818CF8)

// Slate Theme
val SlateBackground = Color(0xFF111827)
val SlateSurface = Color(0xFF1F2937)
val SlateSurfaceVariant = Color(0xFF374151)
val SlatePrimary = Color(0xFF60A5FA)

// AMOLED Theme
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF121212)
val AmoledSurfaceVariant = Color(0xFF1E1E1E)
val AmoledPrimary = Color(0xFFF43F5E)

// Mint Theme
val MintBackground = Color(0xFF06201A)
val MintSurface = Color(0xFF0C382E)
val MintSurfaceVariant = Color(0xFF165244)
val MintPrimary = Color(0xFF10B981)

// Sunset Theme
val SunsetBackground = Color(0xFF1F1212)
val SunsetSurface = Color(0xFF331D1D)
val SunsetSurfaceVariant = Color(0xFF4A2A2A)
val SunsetPrimary = Color(0xFFF97316)

// Priority colors
val PriorityLowColor = Color(0xFF10B981)
val PriorityMediumColor = Color(0xFF3B82F6)
val PriorityHighColor = Color(0xFFF59E0B)
val PriorityUrgentColor = Color(0xFFEF4444)

fun getWallpaperBrush(preset: String): androidx.compose.ui.graphics.Brush {
    return when (preset.uppercase()) {
        "AURORA" -> androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(Color(0xFF031A15), Color(0xFF064E3B), Color(0xFF0F172A))
        )
        "EMBER" -> androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(Color(0xFF1F0E0E), Color(0xFF451A03), Color(0xFF1C1917))
        )
        "SLATE" -> androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
        )
        "PITCH_BLACK" -> androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(Color.Black, Color.Black)
        )
        else -> androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(Color(0xFF090D1A), Color(0xFF1E1B4B), Color(0xFF0F172A))
        )
    }
}
