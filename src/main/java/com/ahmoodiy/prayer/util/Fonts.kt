package com.ahmoodiy.prayer.util

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.ahmoodiy.prayer.R
import com.ahmoodiy.prayer.domain.model.ArabicFont
import androidx.compose.ui.text.googlefonts.Font as GoogleFontFile

/** Google Fonts family names backing each of the app typefaces. */
private val googleFamilyNames: Map<ArabicFont, String> = mapOf(
    ArabicFont.AMIRI to "Amiri",
    ArabicFont.RUQAA to "Aref Ruqaa",
    ArabicFont.KUFI to "Noto Kufi Arabic",
    ArabicFont.TAJAWAL to "Tajawal",
    ArabicFont.CAIRO to "Cairo",
)

/**
 * Resolves the selected Arabic typeface through the Google Fonts downloadable-font provider.
 * When the provider is unavailable (no Play Services or first run while offline) Compose falls
 * back to the platform font, keeping the app fully usable offline.
 */
@OptIn(ExperimentalTextApi::class)
fun ArabicFont.resolveFontFamily(): FontFamily {
    val name = googleFamilyNames[this] ?: return FontFamily.Default
    val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )
    val font = GoogleFont(name)
    return FontFamily(
        GoogleFontFile(googleFont = font, fontProvider = provider, weight = FontWeight.Normal),
        GoogleFontFile(googleFont = font, fontProvider = provider, weight = FontWeight.Medium),
        GoogleFontFile(googleFont = font, fontProvider = provider, weight = FontWeight.SemiBold),
        GoogleFontFile(googleFont = font, fontProvider = provider, weight = FontWeight.Bold),
        GoogleFontFile(googleFont = font, fontProvider = provider, weight = FontWeight.Black),
    )
}
