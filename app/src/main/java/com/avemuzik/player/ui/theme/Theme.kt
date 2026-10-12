package com.avemuzik.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.avemuzik.player.model.ThemeMode

// Ave-Müzik paleti: koyu, sakin bir zemin üzerinde lavanta tonlu bir vurgu.
private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4A7FF),
    onPrimary = Color(0xFF1E1550),
    primaryContainer = Color(0xFF3A2F86),
    onPrimaryContainer = Color(0xFFE5DFFF),
    secondary = Color(0xFFC6C1DC),
    onSecondary = Color(0xFF2E2B41),
    secondaryContainer = Color(0xFF2A2833),
    onSecondaryContainer = Color(0xFFE3DFF6),
    tertiary = Color(0xFFFFB3C6),
    onTertiary = Color(0xFF4E1A2B),
    background = Color(0xFF0D0E11),
    onBackground = Color(0xFFE7E8EC),
    surface = Color(0xFF0D0E11),
    onSurface = Color(0xFFE7E8EC),
    surfaceVariant = Color(0xFF23252C),
    onSurfaceVariant = Color(0xFF9EA2AD),
    surfaceContainerLowest = Color(0xFF08090B),
    surfaceContainerLow = Color(0xFF121317),
    surfaceContainer = Color(0xFF16181C),
    surfaceContainerHigh = Color(0xFF1D1F25),
    surfaceContainerHighest = Color(0xFF262830),
    outline = Color(0xFF41444D),
    outlineVariant = Color(0xFF2B2D34),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B49C9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5DFFF),
    onPrimaryContainer = Color(0xFF1A0E5C),
    secondary = Color(0xFF5E5A72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE4DFF9),
    onSecondaryContainer = Color(0xFF1B182B),
    tertiary = Color(0xFF9A3F5B),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F7FA),
    onBackground = Color(0xFF1A1B20),
    surface = Color(0xFFF7F7FA),
    onSurface = Color(0xFF1A1B20),
    surfaceVariant = Color(0xFFE4E3EC),
    onSurfaceVariant = Color(0xFF5D5E68),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F1F6),
    surfaceContainer = Color(0xFFECECF2),
    surfaceContainerHigh = Color(0xFFE6E6ED),
    surfaceContainerHighest = Color(0xFFE0E0E8),
    outline = Color(0xFF8E8F99),
    outlineVariant = Color(0xFFC9C8D2),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

private val BaseTypography = Typography()

private val AveTypography = BaseTypography.copy(
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
}

@Composable
fun AveMuzikTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AveTypography,
        content = content,
    )
}
