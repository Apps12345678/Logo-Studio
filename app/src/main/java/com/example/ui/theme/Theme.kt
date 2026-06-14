package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val ElegantDarkColorScheme = darkColorScheme(
    primary = ElegantDarkAccent,
    onPrimary = ElegantDarkOnAccent,
    secondary = ElegantDarkBorder,
    onSecondary = ElegantDarkTextPrimary,
    background = ElegantDarkBg,
    onBackground = ElegantDarkTextPrimary,
    surface = ElegantDarkSurface,
    onSurface = ElegantDarkTextPrimary,
    surfaceVariant = ElegantDarkContainer,
    onSurfaceVariant = ElegantDarkTextSecondary,
    outline = ElegantDarkBorder,
    secondaryContainer = ElegantDarkContainer,
    onSecondaryContainer = ElegantDarkTextPrimary,
    primaryContainer = ElegantDarkAccent,
    onPrimaryContainer = ElegantDarkOnAccent
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  // Always use the exquisite Elegant Dark theme signature
  val colorScheme = ElegantDarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
