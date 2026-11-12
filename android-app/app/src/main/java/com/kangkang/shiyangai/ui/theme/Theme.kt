package com.kangkang.shiyangai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 温润食养国风设计令牌，供 Compose 槽位之外的语义色直接引用。 */
object KangKangColors {
    val Paper = Color(0xFFF7F2E7)
    val Card = Color(0xFFFFFDF7)
    val Primary = Color(0xFFB3541E)
    val PrimaryPressed = Color(0xFF8F4218)
    val PrimaryTint = Color(0xFFF4E4D4)
    val Secondary = Color(0xFF4E6E58)
    val SecondaryTint = Color(0xFFE7EFE7)
    val Ink = Color(0xFF3A3226)
    val InkSoft = Color(0xFF74685A)
    val InkFaint = Color(0xFF8C8275)
    val Line = Color(0xFFE4D9C3)
}

private val LightColors = lightColorScheme(
    primary = KangKangColors.Primary,
    onPrimary = Color.White,
    primaryContainer = KangKangColors.PrimaryTint,
    onPrimaryContainer = KangKangColors.Ink,
    secondary = KangKangColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = KangKangColors.SecondaryTint,
    onSecondaryContainer = KangKangColors.Secondary,
    background = KangKangColors.Paper,
    onBackground = KangKangColors.Ink,
    surface = KangKangColors.Card,
    onSurface = KangKangColors.Ink,
    surfaceVariant = KangKangColors.PrimaryTint,
    onSurfaceVariant = KangKangColors.InkSoft,
    outline = KangKangColors.Line,
    outlineVariant = KangKangColors.Line,
    surfaceTint = KangKangColors.Primary
)

private val KangKangTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 42.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 30.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 20.sp,
        lineHeight = 32.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

private val KangKangShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun KangKangTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = KangKangTypography,
        shapes = KangKangShapes,
        content = content
    )
}
