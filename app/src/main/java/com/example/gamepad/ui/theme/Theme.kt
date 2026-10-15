package com.example.gamepad.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF57DDCA),
    onPrimary = Color(0xFF0A101C),
    secondary = Color(0xFF79B8FF),
    background = Color(0xFF0A101C),
    surface = Color(0xFF141F30),
    onSurface = Color(0xFFEAF1FC),
    onBackground = Color(0xFFEAF1FC),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF536F82),
    onPrimary = Color(0xFFFFFAF3),
    primaryContainer = Color(0xFFB8D4E5),
    onPrimaryContainer = Color(0xFF414B53),
    secondary = Color(0xFF657853),
    secondaryContainer = Color(0xFFD2DDC2),
    tertiary = Color(0xFFB16E71),
    tertiaryContainer = Color(0xFFF2C1BC),
    background = Color(0xFFC7DDE6),
    onBackground = Color(0xFF514F50),
    surface = Color(0xFFFFF8EF),
    onSurface = Color(0xFF514F50),
    onSurfaceVariant = Color(0xFF6C7071),
    outline = Color(0xFF888787),
    outlineVariant = Color(0xFFD6CECA),
    surfaceContainerHigh = Color(0xFFFFF8EF),
    surfaceContainerHighest = Color(0xFFF0E8E0),
    error = Color(0xFFA44E54),

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun GamePadTheme(
    darkTheme: Boolean = false,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
