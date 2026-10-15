package com.ahmoodiy.prayer.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ahmoodiy.prayer.PrayerApp
import com.ahmoodiy.prayer.audio.AdhanPlayer
import com.ahmoodiy.prayer.audio.BeadSoundPlayer
import com.ahmoodiy.prayer.audio.CustomAdhanStore
import com.ahmoodiy.prayer.data.prefs.SettingsStore
import com.ahmoodiy.prayer.data.prefs.UserDataStore
import com.ahmoodiy.prayer.util.BackupManager
import com.ahmoodiy.prayer.util.Haptics

/**
 * Hand-rolled dependency container.
 *
 * The app is deliberately dependency-injection-framework free: a single container lives on the
 * [PrayerApp] instance and is handed to every ViewModel through a [viewModelFactory].
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /** Application context for services / receivers that need one. */
    val context: Context get() = appContext

    val settingsStore: SettingsStore = SettingsStore(appContext)
    val userDataStore: UserDataStore = UserDataStore(appContext)

    val beadSoundPlayer: BeadSoundPlayer = BeadSoundPlayer(appContext)
    val adhanPlayer: AdhanPlayer = AdhanPlayer(appContext)
    val customAdhanStore: CustomAdhanStore = CustomAdhanStore(appContext)

    val backupManager: BackupManager = BackupManager(appContext, settingsStore, userDataStore)

    val haptics: Haptics = Haptics(appContext)
}

/** Retrieves the container from the [CreationExtras] available to every ViewModel. */
fun CreationExtras.appContainer(): AppContainer {
    val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
    return (application as PrayerApp).container
}

/** Convenience for building a factory that only needs the container. */
inline fun <reified VM : androidx.lifecycle.ViewModel> containerViewModelFactory(
    crossinline create: (AppContainer) -> VM,
) = viewModelFactory {
    initializer { create(appContainer()) }
}
