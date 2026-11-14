package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val WahjLightColorScheme = lightColorScheme(
    primary = WahjBlue,
    onPrimary = WahjWhite,
    primaryContainer = WahjBlueSoft,
    onPrimaryContainer = WahjBlue,
    secondary = WahjBlack,
    onSecondary = WahjWhite,
    secondaryContainer = WahjOffWhite,
    onSecondaryContainer = WahjBlack,
    tertiary = WahjGray,
    onTertiary = WahjWhite,
    background = WahjWhite,
    onBackground = WahjBlack,
    surface = WahjWhite,
    onSurface = WahjBlack,
    surfaceVariant = WahjOffWhite,
    onSurfaceVariant = WahjGray,
    outline = WahjCardBorderLight,
    outlineVariant = WahjCardBorderLight,
    error = WahjAmberNotice,
    onError = WahjWhite,
    errorContainer = WahjAmberSoft,
    onErrorContainer = WahjAmberNotice
)

private val WahjDarkColorScheme = darkColorScheme(
    primary = WahjBlueLight,
    onPrimary = WahjWhite,
    primaryContainer = WahjBlueDarkSoft,
    onPrimaryContainer = WahjBlueLight,
    secondary = WahjWhite,
    onSecondary = WahjBlack,
    secondaryContainer = WahjDarkSurfaceVariant,
    onSecondaryContainer = WahjWhite,
    tertiary = WahjGrayLight,
    onTertiary = WahjBlack,
    background = WahjDarkBg,
    onBackground = WahjWhite,
    surface = WahjDarkSurface,
    onSurface = WahjWhite,
    surfaceVariant = WahjDarkSurfaceVariant,
    onSurfaceVariant = WahjGrayLight,
    outline = WahjDarkBorder,
    outlineVariant = WahjDarkBorder,
    error = WahjAmberNotice,
    onError = WahjWhite,
    errorContainer = WahjDarkSurfaceVariant,
    onErrorContainer = WahjAmberSoft
)

val WahjShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun WahjTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) WahjDarkColorScheme else WahjLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = WahjTypography,
        shapes = WahjShapes,
        content = content
    )
}
