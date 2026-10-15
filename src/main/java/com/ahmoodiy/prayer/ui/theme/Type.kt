package com.ahmoodiy.prayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Builds the app typography on top of the user-selected Arabic typeface.
 * Sizes roughly mirror the web build's Tailwind scale.
 */
fun buildTypography(family: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontFamily = family, fontWeight = FontWeight.Bold),
        titleSmall = base.titleSmall.copy(fontFamily = family, fontWeight = FontWeight.Bold),
        bodyLarge = bodyLarge(family),
        bodyMedium = bodyMedium(family),
        bodySmall = bodySmall(family),
        labelLarge = base.labelLarge.copy(fontFamily = family, fontWeight = FontWeight.Bold),
        labelMedium = base.labelMedium.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
        labelSmall = base.labelSmall.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
    )
}

/** Sacred text style: comfortable Arabic reading rhythm. */
fun bodyLarge(family: FontFamily) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    lineHeight = 27.sp,
)

fun bodyMedium(family: FontFamily) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 24.sp,
)

fun bodySmall(family: FontFamily) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 20.sp,
)

/** Monospaced-style digits for the live countdown blocks. */
val TimerTextStyle = TextStyle(
    fontWeight = FontWeight.Black,
    fontSize = 34.sp,
    lineHeight = 38.sp,
    letterSpacing = 1.sp,
)
