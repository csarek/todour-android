package com.example.todour

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// --- Világos téma ---
private val TodourLightColors = lightColorScheme(
    primary = Color(0xFF5B6EF5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1E4FF),
    onPrimaryContainer = Color(0xFF1B1F5C),
    secondary = Color(0xFFE8A33D),
    onSecondary = Color.White,
    background = Color(0xFFFBFAF7),
    surface = Color(0xFFFBFAF7),
    surfaceVariant = Color(0xFFF1F0EC),
    onSurfaceVariant = Color(0xFF48453C),
)

// --- Sötét téma: Logseq-szerű, sötétzöldes árnyalat ---
private val TodourDarkColors = darkColorScheme(
    primary = Color(0xFF6FBF8F),
    onPrimary = Color(0xFF0A1F15),
    primaryContainer = Color(0xFF2E4A3B),
    onPrimaryContainer = Color(0xFFCFF3DD),
    secondary = Color(0xFFE0B65D),
    onSecondary = Color(0xFF241A00),
    background = Color(0xFF15201B),
    surface = Color(0xFF15201B),
    surfaceVariant = Color(0xFF22302A),
    onSurfaceVariant = Color(0xFFD3E0D8),
    onBackground = Color(0xFFE3ECE6),
    onSurface = Color(0xFFE3ECE6),
)

private val TodourTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
)

@Composable
fun TodourTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) TodourDarkColors else TodourLightColors,
        typography = TodourTypography,
        content = content
    )
}
