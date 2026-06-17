package com.animesh.timetable.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.animesh.timetable.data.local.ThemeMode

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoSoft,
    onPrimaryContainer = IndigoDark,
    secondary = Indigo,
    tertiary = Color(0xFF2BA89E),
    background = NeutralBg,
    onBackground = Color(0xFF1A1A1F),
    surface = NeutralSurface,
    onSurface = Color(0xFF1A1A1F),
    surfaceVariant = Color(0xFFF1F1F6),
    onSurfaceVariant = Color(0xFF6B6B76),
    outlineVariant = NeutralLine,
    error = Bad
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9A9F2),
    onPrimary = Color(0xFF1A1A40),
    primaryContainer = IndigoDark,
    onPrimaryContainer = IndigoSoft,
    secondary = Color(0xFFA9A9F2),
    tertiary = Color(0xFF66C9BF),
    background = DarkBg,
    onBackground = Color(0xFFE6E6EC),
    surface = DarkSurface,
    onSurface = Color(0xFFE6E6EC),
    surfaceVariant = Color(0xFF26262E),
    onSurfaceVariant = Color(0xFF9A9AA6),
    outlineVariant = DarkLine,
    error = Color(0xFFE7706E)
)

private val AppTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
fun TimeTableTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
