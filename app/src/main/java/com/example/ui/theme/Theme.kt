package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1D1B20),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color.White,
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFEADDFF).copy(alpha = 0.5f),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFCAC4D0)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Default to false to preserve our "Sleek Interface" visual brand identity
  dynamicColor: Boolean = false,
  customThemeEnabled: Boolean = false,
  customPrimary: Color = Color(0xFF6750A4),
  customSecondary: Color = Color(0xFF625B71),
  customBackground: Color = Color(0xFFFEF7FF),
  customSurface: Color = Color(0xFFFFFFFF),
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      customThemeEnabled -> {
        val isDark = isColorDark(customBackground)
        val computedSurfaceVariant = getSurfaceVariant(customSurface, isDark)
        val computedOnSurfaceVariant = getOnSurfaceVariant(computedSurfaceVariant, isDark)
        val computedOutline = getOutline(customSurface, isDark)
        val computedOnBackground = if (isDark) Color.White else Color.Black
        val computedOnSurface = if (isColorDark(customSurface)) Color.White else Color.Black

        if (isDark) {
          darkColorScheme(
            primary = customPrimary,
            onPrimary = if (isColorDark(customPrimary)) Color.White else Color.Black,
            primaryContainer = customPrimary.copy(alpha = 0.2f),
            onPrimaryContainer = customPrimary,
            secondary = customSecondary,
            onSecondary = if (isColorDark(customSecondary)) Color.White else Color.Black,
            secondaryContainer = customSecondary.copy(alpha = 0.2f),
            onSecondaryContainer = customSecondary,
            background = customBackground,
            onBackground = computedOnBackground,
            surface = customSurface,
            onSurface = computedOnSurface,
            surfaceVariant = computedSurfaceVariant,
            onSurfaceVariant = computedOnSurfaceVariant,
            outline = computedOutline
          )
        } else {
          lightColorScheme(
            primary = customPrimary,
            onPrimary = if (isColorDark(customPrimary)) Color.White else Color.Black,
            primaryContainer = customPrimary.copy(alpha = 0.2f),
            onPrimaryContainer = customPrimary,
            secondary = customSecondary,
            onSecondary = if (isColorDark(customSecondary)) Color.White else Color.Black,
            secondaryContainer = customSecondary.copy(alpha = 0.2f),
            onSecondaryContainer = customSecondary,
            background = customBackground,
            onBackground = computedOnBackground,
            surface = customSurface,
            onSurface = computedOnSurface,
            surfaceVariant = computedSurfaceVariant,
            onSurfaceVariant = computedOnSurfaceVariant,
            outline = computedOutline
          )
        }
      }

      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

private fun isColorDark(color: Color): Boolean {
  val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
  return luminance < 0.5f
}

private fun getSurfaceVariant(surface: Color, isDark: Boolean): Color {
  return if (isDark) {
    // Make it slightly lighter for nice elevation/depth
    val factor = 0.10f
    Color(
      red = (surface.red + factor).coerceAtMost(1f),
      green = (surface.green + factor).coerceAtMost(1f),
      blue = (surface.blue + factor).coerceAtMost(1f),
      alpha = surface.alpha
    )
  } else {
    // Make it slightly darker for contrast
    val factor = 0.08f
    Color(
      red = (surface.red - factor).coerceAtLeast(0f),
      green = (surface.green - factor).coerceAtLeast(0f),
      blue = (surface.blue - factor).coerceAtLeast(0f),
      alpha = surface.alpha
    )
  }
}

private fun getOnSurfaceVariant(surfaceVariant: Color, isDark: Boolean): Color {
  return if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF49454F)
}

private fun getOutline(surface: Color, isDark: Boolean): Color {
  return if (isDark) Color.White.copy(alpha = 0.3f) else Color(0xFFCAC4D0)
}
