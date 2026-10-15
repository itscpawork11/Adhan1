package com.ahmoodiy.prayer.alarm

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ahmoodiy.prayer.MainActivity
import com.ahmoodiy.prayer.R
import com.ahmoodiy.prayer.domain.model.PrayerKey

/** Builds the notifications shown by [AdhanService]. */
object NotificationFactory {

    fun buildPrayerNotification(
        context: Context,
        prayerKey: String,
        isPreReminder: Boolean,
        minutesBefore: Int = 15,
    ): Notification {
        val prayer = PrayerKey.fromId(prayerKey)
        val prayerName = prayer?.arabicName ?: prayerKey

        val contentIntent = PendingIntent.getActivity(
            context,
            prayerKey.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title: String
        val text: String
        if (isPreReminder) {
            title = context.getString(R.string.adhan_pre_title, prayerName, minutesBefore)
            text = context.getString(R.string.adhan_pre_text, prayerName)
        } else {
            title = context.getString(R.string.adhan_now_title, prayerName)
            text = context.getString(R.string.adhan_now_text, prayerName)
        }

        return NotificationCompat.Builder(context, NotificationChannels.PRAYER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .build()
    }
}
