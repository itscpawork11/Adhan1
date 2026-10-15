package com.ahmoodiy.prayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ahmoodiy.prayer.ui.AppViewModel
import com.ahmoodiy.prayer.ui.LocalAppSettings
import com.ahmoodiy.prayer.ui.navigation.AppNav
import com.ahmoodiy.prayer.ui.theme.AhmoodiyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
            val settings by appViewModel.settings.collectAsStateWithLifecycle()

            AhmoodiyTheme(font = settings.font) {
                CompositionLocalProvider(LocalAppSettings provides settings) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        AppNav()
                    }
                }
            }
        }
    }
}
