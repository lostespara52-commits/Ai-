package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DuaFatimaDarkColorScheme = darkColorScheme(
    primary = DuaCrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF420A17),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = DuaRubyBright,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF380C18),
    onSecondaryContainer = DuaRoseGold,
    tertiary = AuraNeonPurple,
    onTertiary = Color.White,
    background = AuraObsidian,
    onBackground = AuraTextPrimary,
    surface = AuraDeepSpace,
    onSurface = AuraTextPrimary,
    surfaceVariant = AuraSurfaceGlass,
    onSurfaceVariant = AuraTextSecondary,
    outline = AuraBorderGlow,
    error = AuraCrimson,
    onError = Color.White
)

private val DuaFatimaLightColorScheme = lightColorScheme(
    primary = Color(0xFFD5002B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9DF),
    onPrimaryContainer = Color(0xFF3D000B),
    secondary = Color(0xFFB71C1C),
    onSecondary = Color.White,
    secondaryContainer = DuaLightSurfaceVariant,
    onSecondaryContainer = Color(0xFF4A000E),
    tertiary = Color(0xFFD84315),
    onTertiary = Color.White,
    background = DuaLightBackground,
    onBackground = DuaLightTextPrimary,
    surface = DuaLightSurface,
    onSurface = DuaLightTextPrimary,
    surfaceVariant = DuaLightSurfaceVariant,
    onSurfaceVariant = DuaLightTextSecondary,
    outline = DuaLightBorder,
    error = Color(0xFFC62828),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DuaFatimaDarkColorScheme else DuaFatimaLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
