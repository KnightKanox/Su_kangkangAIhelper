package com.kangkang.shiyangai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    secondary = Color(0xFF66BB6A),
    background = Color(0xFFFAF9F4),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF3EE)
)

@Composable
fun KangKangTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
