package xyz.jishnu.health.ui.theme

import androidx.compose.ui.graphics.Color

data class IntermColors(
    val bg: Color,
    val surface: Color,
    val card: Color,
    val ink: Color,
    val ink2: Color,
    val muted: Color,
    val subtle: Color,
    val border: Color,
    val border2: Color,
    val primary: Color,
    val primary2: Color,
    val primarySoft: Color,
    val accent: Color,
    val accentSoft: Color,
    val danger: Color,
    val warn: Color,
    val isDark: Boolean,
)

val LightIntermColors = IntermColors(
    bg = Color(0xFFF6F3EE),
    surface = Color(0xFFFDFBF7),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF14130F),
    ink2 = Color(0xFF3A3A36),
    muted = Color(0xFF8A857C),
    subtle = Color(0xFFB8B3A7),
    border = Color(0xFFE8E3D8),
    border2 = Color(0xFFF0ECDF),
    primary = Color(0xFF2A4D3E),
    primary2 = Color(0xFF3D6B56),
    primarySoft = Color(0xFFE7EEE8),
    accent = Color(0xFFD97757),
    accentSoft = Color(0xFFF7E7DF),
    danger = Color(0xFFB54734),
    warn = Color(0xFFC69142),
    isDark = false,
)

val DarkIntermColors = IntermColors(
    bg = Color(0xFF14130F),
    surface = Color(0xFF1C1B17),
    card = Color(0xFF24221D),
    ink = Color(0xFFF6F3EE),
    ink2 = Color(0xFFD8D3C8),
    muted = Color(0xFF8A857C),
    subtle = Color(0xFF5A574F),
    border = Color(0xFF2D2A23),
    border2 = Color(0xFF26241E),
    primary = Color(0xFF7DD3A8),
    primary2 = Color(0xFF9ADBB7),
    primarySoft = Color(0xFF1F2A23),
    accent = Color(0xFFE89074),
    accentSoft = Color(0xFF2E211B),
    danger = Color(0xFFE87164),
    warn = Color(0xFFD9A35A),
    isDark = true,
)
