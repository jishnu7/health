package xyz.jishnu.health.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object IntermTheme {
    val colors: IntermColors
        @Composable @ReadOnlyComposable get() = LocalIntermColors.current

    val typography: IntermTypography
        @Composable @ReadOnlyComposable get() = LocalIntermTypography.current
}

private fun materialFrom(c: IntermColors) = if (c.isDark) {
    darkColorScheme(
        background = c.bg,
        surface = c.surface,
        onBackground = c.ink,
        onSurface = c.ink,
        primary = c.primary,
        onPrimary = c.bg,
        secondary = c.accent,
        onSecondary = c.bg,
        error = c.danger,
        outline = c.border,
    )
} else {
    lightColorScheme(
        background = c.bg,
        surface = c.surface,
        onBackground = c.ink,
        onSurface = c.ink,
        primary = c.primary,
        onPrimary = c.surface,
        secondary = c.accent,
        onSecondary = c.surface,
        error = c.danger,
        outline = c.border,
    )
}

@Composable
fun IntermTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkIntermColors else LightIntermColors
    CompositionLocalProvider(
        LocalIntermColors provides colors,
        LocalIntermTypography provides IntermTextStyles,
    ) {
        MaterialTheme(
            colorScheme = materialFrom(colors),
            typography = Typography(
                bodyMedium = IntermTextStyles.body,
                bodySmall = IntermTextStyles.caption,
                titleLarge = IntermTextStyles.hTitle,
                titleMedium = IntermTextStyles.headerTitle,
                labelLarge = IntermTextStyles.button,
            ),
            content = content,
        )
    }
}
