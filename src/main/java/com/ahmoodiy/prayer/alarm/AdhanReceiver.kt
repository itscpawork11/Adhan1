package com.ahmoodiy.prayer.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Fired by [android.app.AlarmManager] at the scheduled prayer time.
 * It only forwards the prayer identity to [AdhanService]; all heavy work happens there.
 */
class AdhanReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val prayerKey = intent?.getStringExtra(EXTRA_PRAYER_KEY) ?: return
        val isPreReminder = intent.getBooleanExtra(EXTRA_IS_PRE_REMINDER, false)

        val serviceIntent = Intent(context, AdhanService::class.java).apply {
            putExtra(EXTRA_PRAYER_KEY, prayerKey)
            putExtra(EXTRA_IS_PRE_REMINDER, isPreReminder)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        const val EXTRA_PRAYER_KEY = "extra_prayer_key"
        const val EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder"
    }
}
