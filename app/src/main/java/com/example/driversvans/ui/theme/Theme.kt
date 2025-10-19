package com.example.driversvans.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.os.Build

private val LightColors = lightColorScheme(
    primary = Purple40,
    onPrimary = White,
    secondary = Purple80,
    onSecondary = Black,
    surface = SurfaceLight,
    onSurface = Black
)

private val DarkColors = darkColorScheme(
    primary = Purple80,
    onPrimary = Black,
    secondary = Purple40,
    onSecondary = White,
    surface = SurfaceDark,
    onSurface = White
)

@Composable
fun DriverVansTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // Keep it simple; you can add dynamic colors later if you want.
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
