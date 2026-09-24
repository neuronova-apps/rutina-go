package com.neuronovaapps.rutinago.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun rutinaGoTypography(scale: Float = 1f) = Typography(
    displaySmall = readableStyle(36f, 44f, FontWeight.Bold, scale),
    headlineLarge = readableStyle(32f, 40f, FontWeight.Bold, scale),
    headlineMedium = readableStyle(28f, 36f, FontWeight.Bold, scale),
    headlineSmall = readableStyle(24f, 32f, FontWeight.Bold, scale),
    titleLarge = readableStyle(22f, 28f, FontWeight.Bold, scale),
    titleMedium = readableStyle(17f, 24f, FontWeight.SemiBold, scale),
    titleSmall = readableStyle(15f, 22f, FontWeight.SemiBold, scale),
    bodyLarge = readableStyle(17f, 26f, FontWeight.Normal, scale),
    bodyMedium = readableStyle(15f, 23f, FontWeight.Normal, scale),
    bodySmall = readableStyle(13f, 20f, FontWeight.Normal, scale),
    labelLarge = readableStyle(15f, 22f, FontWeight.Bold, scale),
    labelMedium = readableStyle(13f, 18f, FontWeight.SemiBold, scale),
    labelSmall = readableStyle(11f, 16f, FontWeight.SemiBold, scale),
)

private fun readableStyle(
    size: Float,
    lineHeight: Float,
    weight: FontWeight,
    scale: Float,
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = (size * scale).sp,
    lineHeight = (lineHeight * scale).sp,
)
