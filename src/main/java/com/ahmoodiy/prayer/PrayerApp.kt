package com.ahmoodiy.prayer

import android.app.Application
import com.ahmoodiy.prayer.alarm.NotificationChannels
import com.ahmoodiy.prayer.di.AppContainer

/**
 * Application entry point.
 *
 * Owns the single [AppContainer] that provides every store / audio helper to the ViewModels,
 * and makes sure the notification channels exist as early as possible.
 */
class PrayerApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
    }
}
