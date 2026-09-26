package com.example.todour

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val TodourPrimary = Color(0xFF5B6EF5)
private val TodourPrimaryContainer = Color(0xFFE1E4FF)
private val TodourOnPrimaryContainer = Color(0xFF1B1F5C)
private val TodourSecondary = Color(0xFFE8A33D)
private val TodourBackground = Color(0xFFFBFAF7)
private val TodourSurfaceVariant = Color(0xFFF1F0EC)
private val TodourOnSurfaceVariant = Color(0xFF48453C)

private val TodourLightColors = lightColorScheme(
    primary = TodourPrimary,
    onPrimary = Color.White,
    primaryContainer = TodourPrimaryContainer,
    onPrimaryContainer = TodourOnPrimaryContainer,
    secondary = TodourSecondary,
    onSecondary = Color.White,
    background = TodourBackground,
    surface = TodourBackground,
    surfaceVariant = TodourSurfaceVariant,
    onSurfaceVariant = TodourOnSurfaceVariant,
)

private val TodourTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    )
)

@Composable
fun TodourTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TodourLightColors,
        typography = TodourTypography,
        content = content
    )
}
