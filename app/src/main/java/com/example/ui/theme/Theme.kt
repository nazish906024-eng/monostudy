package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

object MonostudyPalette {
    val Bg = Color(0xFF0B1020)
    val Panel = Color(0xFF141B33)
    val PanelElevated = Color(0xFF1B2442)
    val Line = Color(0xFF27325A)
    val TextPrimary = Color(0xFFEEF1FF)
    val TextMuted = Color(0xFF8F9BC4)
    val Primary = Color(0xFF8B7DFF)
    val PrimaryDeep = Color(0xFF6A5BF0)
    val AccentBlue = Color(0xFF4AA3FF)
    val AccentGreen = Color(0xFF3DDC97)
    val AccentOrange = Color(0xFFFFA94D)
    val AccentRed = Color(0xFFFF6B7A)
    val HeatLow = Color(0xFF7A2230)
    val HeatMidLow = Color(0xFF8A5A1D)
    val HeatMidHigh = Color(0xFF2D6A56)
    val HeatHigh = Color(0xFF1F8A5E)
}

val SpaceGroteskFontFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val PlusJakartaFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

private val MonostudyDarkColorScheme = darkColorScheme(
    primary = MonostudyPalette.Primary,
    onPrimary = MonostudyPalette.Bg,
    primaryContainer = MonostudyPalette.PrimaryDeep,
    onPrimaryContainer = MonostudyPalette.TextPrimary,
    secondary = MonostudyPalette.AccentBlue,
    onSecondary = MonostudyPalette.Bg,
    secondaryContainer = MonostudyPalette.PanelElevated,
    onSecondaryContainer = MonostudyPalette.TextPrimary,
    tertiary = MonostudyPalette.AccentGreen,
    onTertiary = MonostudyPalette.Bg,
    background = MonostudyPalette.Bg,
    onBackground = MonostudyPalette.TextPrimary,
    surface = MonostudyPalette.Panel,
    onSurface = MonostudyPalette.TextPrimary,
    surfaceVariant = MonostudyPalette.PanelElevated,
    onSurfaceVariant = MonostudyPalette.TextMuted,
    outline = MonostudyPalette.Line,
    outlineVariant = MonostudyPalette.Line,
    error = MonostudyPalette.AccentRed,
    onError = Color(0xFF1A0509)
)

val MonostudyTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

val MonostudyShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun MonostudyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MonostudyDarkColorScheme,
        typography = MonostudyTypography,
        shapes = MonostudyShapes,
        content = content
    )
}
