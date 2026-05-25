package xyz.jishnu.health.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    background = Color(0xFFF6F3EE),
    surface = Color(0xFFFDFBF7),
    onBackground = Color(0xFF14130F),
    onSurface = Color(0xFF14130F),
    primary = Color(0xFF2A4D3E),
    onPrimary = Color(0xFFFDFBF7),
)

private val DarkScheme = darkColorScheme(
    background = Color(0xFF14130F),
    surface = Color(0xFF1C1B17),
    onBackground = Color(0xFFF6F3EE),
    onSurface = Color(0xFFF6F3EE),
    primary = Color(0xFF7DD3A8),
    onPrimary = Color(0xFF14130F),
)

@Composable
fun IntermTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content,
    )
}
