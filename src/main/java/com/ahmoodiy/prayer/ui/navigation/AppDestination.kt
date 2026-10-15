package com.ahmoodiy.prayer.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector
import com.ahmoodiy.prayer.R

/** The five top-level sections reachable from the bottom navigation bar. */
enum class AppDestination(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
) {
    TASBEEH("tasbeeh", R.string.nav_tasbeeh, Icons.Filled.AutoAwesome),
    ATHKAR("athkar", R.string.nav_athkar, Icons.AutoMirrored.Filled.MenuBook),
    PRAYERS("prayers", R.string.nav_prayers, Icons.Filled.Schedule),
    DUAS("duas", R.string.nav_duas, Icons.Filled.VolunteerActivism),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings),
}
