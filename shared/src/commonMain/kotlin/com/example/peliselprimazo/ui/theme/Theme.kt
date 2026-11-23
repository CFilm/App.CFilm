package com.example.peliselprimazo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun parseHexColor(hex: String?, default: Color): Color {
    if (hex.isNullOrBlank()) return default
    return try {
        val cleaned = hex.removePrefix("#")
        val colorInt = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        default
    }
}

@Composable
fun CFilmTheme(
    primaryOverride: String? = null,
    secondaryOverride: String? = null,
    content: @Composable () -> Unit
) {
    val dynamicPrimary = parseHexColor(primaryOverride, RedPrimazo)
    val dynamicSecondary = parseHexColor(secondaryOverride, GoldPrimazo)

    val colorScheme = darkColorScheme(
        primary = dynamicPrimary,
        secondary = dynamicSecondary,
        background = BlackBackground,
        surface = DarkGraySurface,
        onPrimary = WhiteText,
        onSecondary = BlackBackground,
        onBackground = WhiteText,
        onSurface = WhiteText
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
