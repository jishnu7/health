package xyz.jishnu.health.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import xyz.jishnu.health.R

private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val Geist = GoogleFont("Geist")
private val GeistMono = GoogleFont("Geist Mono")

val GeistFamily = FontFamily(
    Font(googleFont = Geist, fontProvider = GoogleFontsProvider, weight = FontWeight.W300),
    Font(googleFont = Geist, fontProvider = GoogleFontsProvider, weight = FontWeight.W400),
    Font(googleFont = Geist, fontProvider = GoogleFontsProvider, weight = FontWeight.W500),
    Font(googleFont = Geist, fontProvider = GoogleFontsProvider, weight = FontWeight.W600),
    Font(googleFont = Geist, fontProvider = GoogleFontsProvider, weight = FontWeight.W700),
)

val GeistMonoFamily = FontFamily(
    Font(googleFont = GeistMono, fontProvider = GoogleFontsProvider, weight = FontWeight.W400),
    Font(googleFont = GeistMono, fontProvider = GoogleFontsProvider, weight = FontWeight.W500),
    Font(googleFont = GeistMono, fontProvider = GoogleFontsProvider, weight = FontWeight.W600),
)

data class IntermTypography(
    val hDisplay: TextStyle,
    val hTitle: TextStyle,
    val hEyebrow: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val headerTitle: TextStyle,
    val button: TextStyle,
    val mono: TextStyle,
)

val IntermTextStyles = IntermTypography(
    hDisplay = TextStyle(
        fontFamily = GeistMonoFamily,
        fontWeight = FontWeight.W400,
        fontSize = 56.sp,
        letterSpacing = (-0.04).em,
        lineHeight = 56.sp,
    ),
    hTitle = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.W500,
        fontSize = 28.sp,
        letterSpacing = (-0.02).em,
        lineHeight = 30.8.sp,
    ),
    hEyebrow = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        letterSpacing = 0.12.em,
    ),
    body = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    caption = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
    ),
    headerTitle = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.W500,
        fontSize = 17.sp,
        letterSpacing = (-0.01).em,
    ),
    button = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.W500,
        fontSize = 15.sp,
    ),
    mono = TextStyle(
        fontFamily = GeistMonoFamily,
        fontWeight = FontWeight.W400,
        fontFeatureSettings = "tnum",
    ),
)
