package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF183D30), onPrimary = Color(0xFFFFFEFA),
    primaryContainer = Color(0xFFDDE9A8), onPrimaryContainer = Color(0xFF183D30),
    secondary = Color(0xFF626B5C), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9EDDD), onSecondaryContainer = Color(0xFF253A2D),
    tertiary = Color(0xFF8D4D17), onTertiary = Color.White,
    background = Color(0xFFF6F3EB), onBackground = Color(0xFF183D30),
    surface = Color(0xFFFFFEFA), onSurface = Color(0xFF183D30),
    surfaceVariant = Color(0xFFEEEFE4), onSurfaceVariant = Color(0xFF626B5C),
    surfaceContainer = Color(0xFFF0EEE5), surfaceContainerHigh = Color(0xFFE9E9DD),
    surfaceContainerHighest = Color(0xFFE4E6D9), surfaceContainerLow = Color(0xFFFAF8F1),
    outline = Color(0xFF737C6A), outlineVariant = Color(0xFFD5D9CC)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFDDE9A8), onPrimary = Color(0xFF183D30),
    primaryContainer = Color(0xFF304D38), onPrimaryContainer = Color(0xFFE1EDB9),
    secondary = Color(0xFFC0C8B4), onSecondary = Color(0xFF253A2D),
    secondaryContainer = Color(0xFF303C31), onSecondaryContainer = Color(0xFFDCE3D3),
    tertiary = Color(0xFFE4B384), onTertiary = Color(0xFF452B16),
    background = Color(0xFF121C17), onBackground = Color(0xFFF0EEE5),
    surface = Color(0xFF1C2820), onSurface = Color(0xFFF0EEE5),
    surfaceVariant = Color(0xFF2A382D), onSurfaceVariant = Color(0xFFBDC6B5),
    surfaceContainer = Color(0xFF223026), surfaceContainerHigh = Color(0xFF29382D),
    surfaceContainerHighest = Color(0xFF334237), surfaceContainerLow = Color(0xFF18231C),
    outline = Color(0xFF919D89), outlineVariant = Color(0xFF3E4E40)
)

@Composable
fun FoodCalorieAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
