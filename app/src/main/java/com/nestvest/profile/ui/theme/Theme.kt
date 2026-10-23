package com.nestvest.profile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    primaryContainer = SoftSage,
    onPrimaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF454D47),
    outline = Color(0xFF707971),
)

@Composable
fun NestVestTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = NestVestTypography, content = content)
}
