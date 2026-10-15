package com.ahmoodiy.prayer.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.ahmoodiy.prayer.domain.model.AppSettings

/**
 * The live settings object, provided once at the top of the tree so any screen can read
 * presentation-only preferences (typeface scale, tashkeel, sanad visibility ...) without
 * spinning up its own observer.
 */
val LocalAppSettings = staticCompositionLocalOf { AppSettings() }
