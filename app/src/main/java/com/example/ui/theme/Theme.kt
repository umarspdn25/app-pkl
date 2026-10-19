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
    primary = Color(0xFF8BB5E8),
    onPrimary = Color(0xFF003062),
    primaryContainer = TsmNavyContainer,
    onPrimaryContainer = TsmOnNavyContainer,
    secondary = Color(0xFFFFB4AB),
    onSecondary = Color(0xFF690005),
    secondaryContainer = Color(0xFF93000A),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFF9CCAFF),
    background = Color(0xFF0F141C),
    surface = Color(0xFF171D26),
    surfaceVariant = Color(0xFF222B38),
    onSurface = Color(0xFFE2E8F0),
    outline = Color(0xFF3B4858)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TsmNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = TsmNavyContainer,
    onPrimaryContainer = TsmOnNavyContainer,
    secondary = TsmRedSecondary,
    onSecondary = Color.White,
    secondaryContainer = TsmRedContainer,
    onSecondaryContainer = TsmOnRedContainer,
    tertiary = TsmBlueAccent,
    background = TsmSurfaceLight,
    surface = TsmSurfaceCard,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    outline = TsmBorderLight
  )

@Composable
fun PklTsmTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Use our curated branding colors by default for consistent brand identity
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) = PklTsmTheme(darkTheme, dynamicColor, content)

