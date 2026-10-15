package com.ahmoodiy.prayer.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.ahmoodiy.prayer.R

/** Central place for every notification channel the app publishes to. */
object NotificationChannels {

    const val PRAYER_CHANNEL_ID = "prayer_times"
    const val ADHAN_SERVICE_NOTIFICATION_ID = 8001

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return

        val existing = manager.getNotificationChannel(PRAYER_CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            PRAYER_CHANNEL_ID,
            context.getString(R.string.channel_prayer_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_prayer_desc)
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }
}
