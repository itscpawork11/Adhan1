package com.ahmoodiy.prayer.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ahmoodiy.prayer.ui.athkar.AthkarScreen
import com.ahmoodiy.prayer.ui.components.AhmoodiyBottomBar
import com.ahmoodiy.prayer.ui.duas.DuasScreen
import com.ahmoodiy.prayer.ui.prayers.PrayersScreen
import com.ahmoodiy.prayer.ui.settings.SettingsScreen
import com.ahmoodiy.prayer.ui.tasbeeh.TasbeehScreen

@Composable
fun AppNav(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppDestination.TASBEEH.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        // The screen header paints its gradient behind the status bar, so the scaffold itself
        // must not reserve top space; the bottom bar still reports its own height.
        contentWindowInsets = WindowInsets(top = 0.dp),
        bottomBar = {
            AhmoodiyBottomBar(
                currentRoute = currentRoute,
                onSelect = { destination -> navController.navigateToTab(destination) },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.TASBEEH.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            // Tabs cross-fade with a slight rise so switching feels deliberate but never janky.
            enterTransition = {
                fadeIn(animationSpec = tween(220)) +
                    slideInVertically(animationSpec = tween(260)) { it / 28 }
            },
            exitTransition = { fadeOut(animationSpec = tween(140)) },
            popEnterTransition = { fadeIn(animationSpec = tween(220)) },
            popExitTransition = { fadeOut(animationSpec = tween(140)) },
        ) {
            composable(AppDestination.TASBEEH.route) { TasbeehScreen() }
            composable(AppDestination.ATHKAR.route) { AthkarScreen() }
            composable(AppDestination.PRAYERS.route) { PrayersScreen() }
            composable(AppDestination.DUAS.route) { DuasScreen() }
            composable(AppDestination.SETTINGS.route) { SettingsScreen() }
        }
    }
}

private fun NavHostController.navigateToTab(destination: AppDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
