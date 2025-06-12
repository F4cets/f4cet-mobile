package com.f4cets.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF9999), // Peach from bg.png
    onPrimary = Color.White,
    secondary = Color(0xFF4D455D), // Variant card color from f4cets
    onSecondary = Color.White,
    tertiary = Color(0xFF6750A4), // M3 expressive accent
    onTertiary = Color.White,
    background = Color(0xFFF5F5F5), // Light gray for contrast
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

@Composable
fun F4cetMobileTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}