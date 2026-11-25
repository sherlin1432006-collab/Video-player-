package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val CinemaDarkColorScheme = darkColorScheme(
    primary = ProjectorCoral,
    onPrimary = OnProjectorCoral,
    primaryContainer = Color(0xFF421908),
    onPrimaryContainer = Color(0xFFFFD8C7),
    secondary = CinemaGold,
    onSecondary = OnCinemaGold,
    secondaryContainer = Color(0xFF3A2906),
    onSecondaryContainer = Color(0xFFFFE5B4),
    tertiary = LaserCyan,
    onTertiary = OnLaserCyan,
    tertiaryContainer = Color(0xFF0A3236),
    onTertiaryContainer = Color(0xFFC4FAFC),
    background = ObsidianBackground,
    onBackground = ObsidianOnSurface,
    surface = ObsidianSurface,
    onSurface = ObsidianOnSurface,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFF07080D),
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianSurfaceContainerHigh,
    surfaceContainerHighest = ObsidianSurfaceContainerHighest,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineVariant,
    error = CinemaError,
    onError = OnCinemaError,
)

private val StudioLightColorScheme = lightColorScheme(
    primary = StudioCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDD1),
    onPrimaryContainer = Color(0xFF3A0E00),
    secondary = StudioGold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE6BA),
    onSecondaryContainer = Color(0xFF2B1B00),
    tertiary = StudioCyan,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCF4F7),
    onTertiaryContainer = Color(0xFF002326),
    background = StudioBackground,
    onBackground = StudioOnSurface,
    surface = StudioSurface,
    onSurface = StudioOnSurface,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = StudioOnSurfaceVariant,
    surfaceContainerLow = Color(0xFFF6F3EE),
    surfaceContainer = StudioSurfaceVariant,
    surfaceContainerHigh = StudioSurfaceContainerHigh,
    outline = StudioOutline,
    outlineVariant = Color(0xFFE0DBD2),
    error = Color(0xFFBA1A2D),
    onError = Color.White,
)

val ReelFlowShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

enum class ThemeMode {
    CINEMA_DARK,
    STUDIO_LIGHT,
    SYSTEM
}

@Composable
fun ReelFlowTheme(
    themeMode: ThemeMode = ThemeMode.CINEMA_DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.CINEMA_DARK -> true
        ThemeMode.STUDIO_LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDark -> CinemaDarkColorScheme
        else -> StudioLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ReelFlowTypography,
        shapes = ReelFlowShapes,
        content = content,
    )
}
