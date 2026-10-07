package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

fun getLauncherColorScheme(themeName: String, isDynamic: Boolean = false): ColorScheme {
    return when (themeName.uppercase()) {
        "OBSIDIAN" -> darkColorScheme(
            primary = ObsidianPrimary,
            onPrimary = Color(0xFF0F172A),
            primaryContainer = Color(0xFF1E3A5F),
            onPrimaryContainer = Color(0xFFBAE6FD),
            secondary = ObsidianAccent,
            onSecondary = Color(0xFF0F172A),
            background = ObsidianBackground,
            onBackground = Color(0xFFF1F5F9),
            surface = ObsidianSurface,
            onSurface = Color(0xFFF1F5F9),
            surfaceVariant = ObsidianSurfaceVariant,
            onSurfaceVariant = Color(0xFF94A3B8)
        )
        "SLATE" -> darkColorScheme(
            primary = SlatePrimary,
            onPrimary = Color(0xFF0F172A),
            background = SlateBackground,
            onBackground = Color(0xFFF9FAFB),
            surface = SlateSurface,
            onSurface = Color(0xFFF9FAFB),
            surfaceVariant = SlateSurfaceVariant,
            onSurfaceVariant = Color(0xFF9CA3AF)
        )
        "AMOLED" -> darkColorScheme(
            primary = AmoledPrimary,
            onPrimary = Color.White,
            background = AmoledBackground,
            onBackground = Color.White,
            surface = AmoledSurface,
            onSurface = Color.White,
            surfaceVariant = AmoledSurfaceVariant,
            onSurfaceVariant = Color(0xFFAAAAAA)
        )
        "MINT" -> darkColorScheme(
            primary = MintPrimary,
            onPrimary = Color.White,
            background = MintBackground,
            onBackground = Color(0xFFECFDF5),
            surface = MintSurface,
            onSurface = Color(0xFFECFDF5),
            surfaceVariant = MintSurfaceVariant,
            onSurfaceVariant = Color(0xFFA7F3D0)
        )
        "SUNSET" -> darkColorScheme(
            primary = SunsetPrimary,
            onPrimary = Color.White,
            background = SunsetBackground,
            onBackground = Color(0xFFFFF7ED),
            surface = SunsetSurface,
            onSurface = Color(0xFFFFF7ED),
            surfaceVariant = SunsetSurfaceVariant,
            onSurfaceVariant = Color(0xFFFED7AA)
        )
        else -> darkColorScheme(
            primary = IndigoPrimary,
            onPrimary = Color.White,
            secondary = IndigoSecondary,
            onSecondary = Color.White,
            background = IndigoBackground,
            onBackground = Color(0xFFF8FAFC),
            surface = IndigoSurface,
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = IndigoSurfaceVariant,
            onSurfaceVariant = Color(0xFF94A3B8)
        )
    }
}

@Composable
fun TaskLaunchTheme(
    themeName: String = "OBSIDIAN",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        getLauncherColorScheme(themeName)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
