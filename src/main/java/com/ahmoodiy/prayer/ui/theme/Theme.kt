package com.ahmoodiy.prayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.ahmoodiy.prayer.domain.model.ArabicFont
import com.ahmoodiy.prayer.util.resolveFontFamily

private val AhmoodiyColors = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = EmeraldDeep,
    primaryContainer = EmeraldSurface,
    onPrimaryContainer = GoldShimmer,
    secondary = GoldShimmer,
    onSecondary = EmeraldDeep,
    secondaryContainer = EmeraldSurfaceVariant,
    onSecondaryContainer = GoldShimmer,
    tertiary = GoldMuted,
    onTertiary = EmeraldDeep,
    background = EmeraldDeep,
    onBackground = TextPrimary,
    surface = EmeraldSurface,
    onSurface = TextPrimary,
    surfaceVariant = EmeraldSurfaceVariant,
    onSurfaceVariant = TextMuted,
    surfaceContainer = EmeraldSurface,
    surfaceContainerHigh = EmeraldSurfaceVariant,
    surfaceContainerHighest = EmeraldSurfaceVariant,
    surfaceContainerLow = EmeraldDeep,
    outline = EmeraldOutline,
    outlineVariant = EmeraldOutline,
    error = DangerRose,
    onError = EmeraldDeep,
    scrim = EmeraldInk,
)

/** Fallback family used before the user font resolves. */
private val FallbackFamily = FontFamily.Default

@Composable
fun AhmoodiyTheme(
    font: ArabicFont = ArabicFont.AMIRI,
    content: @Composable () -> Unit,
) {
    val family = remember(font) {
        runCatching { font.resolveFontFamily() }.getOrDefault(FallbackFamily)
    }
    val typography = remember(family) { buildTypography(family) }

    // The whole app is Arabic-first and must always lay out right-to-left.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = AhmoodiyColors,
            typography = typography,
            content = content,
        )
    }
}
