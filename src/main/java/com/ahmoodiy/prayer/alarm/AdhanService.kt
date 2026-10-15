package com.ahmoodiy.prayer.alarm

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.ahmoodiy.prayer.audio.AdhanPlayer
import com.ahmoodiy.prayer.audio.CustomAdhanStore
import com.ahmoodiy.prayer.data.prefs.SettingsStore
import com.ahmoodiy.prayer.domain.model.AdhanVoice
import com.ahmoodiy.prayer.domain.model.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.File

/**
 * Foreground service that publishes the prayer notification and plays the selected adhan.
 *
 * Running as a foreground service guarantees the adhan is not throttled or dropped while the
 * screen is off, and the notification doubles as the pre-prayer reminder.
 *
 * Note: everything that touches DataStore runs inside [scope], never on the main thread — the
 * synchronous [onStartCommand] only validates its input and hands the work over.
 */
class AdhanService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var adhanPlayer: AdhanPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerKey = intent?.getStringExtra(AdhanReceiver.EXTRA_PRAYER_KEY)
        val isPreReminder = intent?.getBooleanExtra(AdhanReceiver.EXTRA_IS_PRE_REMINDER, false) == true

        if (prayerKey.isNullOrBlank()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        NotificationChannels.ensureCreated(this)
        scope.launch { deliver(prayerKey, isPreReminder, startId) }
        return START_NOT_STICKY
    }

    private suspend fun deliver(prayerKey: String, isPreReminder: Boolean, startId: Int) {
        val settings: AppSettings? = runCatching {
            withTimeout(3_000) { SettingsStore(applicationContext).settings.first() }
        }.getOrNull()

        val notification = NotificationFactory.buildPrayerNotification(
            context = this,
            prayerKey = prayerKey,
            isPreReminder = isPreReminder,
            minutesBefore = settings?.prayerAlertTiming ?: 15,
        )

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NotificationChannels.ADHAN_SERVICE_NOTIFICATION_ID,
            notification,
            type,
        )

        if (isPreReminder) {
            // The reminder *is* the notification; there is no audio to play.
            stopSelf(startId)
            return
        }

        val customFile: File? = CustomAdhanStore(applicationContext).customFile()
        val player = AdhanPlayer(applicationContext).also { adhanPlayer = it }

        val started = player.play(
            voice = settings?.adhanVoice ?: AdhanVoice.ABDULBASIT,
            customFile = customFile,
            useAlarmStream = true,
        ) { stopSelf(startId) }

        if (!started) {
            stopSelf(startId)
        } else {
            // Hard ceiling so a very long custom clip cannot hold the service forever.
            delay(MAX_PLAYBACK_MS)
            stopSelf(startId)
        }
    }

    override fun onDestroy() {
        adhanPlayer?.stop()
        adhanPlayer = null
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val MAX_PLAYBACK_MS = 5 * 60_000L
    }
}
