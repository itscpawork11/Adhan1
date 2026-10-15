package com.ahmoodiy.prayer.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.ahmoodiy.prayer.data.GovernoratesData
import com.ahmoodiy.prayer.data.prefs.SettingsStore
import com.ahmoodiy.prayer.domain.model.PrayerKey
import com.ahmoodiy.prayer.domain.prayer.EgyptPrayerEngine
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Thin wrapper around [AlarmManager] that is resilient to the Android 12+ exact-alarm
 * restrictions and the Android 14+ foreground-service rules.
 *
 * Scheduling always covers today *and* tomorrow so a reboot or a missing boot broadcast
 * cannot leave the user without a prayer alarm for the next 24 hours.
 */
object AdhanAlarmScheduler {

    const val ACTION_PRAYER_ALARM = "com.ahmoodiy.prayer.ACTION_PRAYER_ALARM"
    private const val MINUTE_MS = 60_000L

    // ----------------------------------------------------------------- one alarm

    /** Schedule a single alarm. Past timestamps are ignored. */
    fun schedule(
        context: Context,
        prayerKey: String,
        triggerAtMillis: Long,
        isPreReminder: Boolean,
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        val manager = context.getSystemService<AlarmManager>() ?: return
        val pendingIntent = buildPendingIntent(context, prayerKey, isPreReminder)

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        try {
            when {
                canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)

                canExact -> manager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)

                else -> manager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            // Exact alarms revoked: fall back to an inexact alarm instead of crashing.
            runCatching { manager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent) }
        }
    }

    fun cancel(context: Context, prayerKey: String, isPreReminder: Boolean) {
        val manager = context.getSystemService<AlarmManager>() ?: return
        runCatching { manager.cancel(buildPendingIntent(context, prayerKey, isPreReminder)) }
    }

    fun cancelAll(context: Context) {
        PrayerKey.prayers.forEach { key ->
            cancel(context, key.id, isPreReminder = false)
            cancel(context, key.id, isPreReminder = true)
        }
    }

    // ----------------------------------------------------------------- full reschedule

    /**
     * Re-computes and re-registers every upcoming prayer alarm from the current settings.
     * Safe to call after any settings change, from the boot receiver, or after an app update.
     */
    suspend fun rescheduleAll(context: Context) {
        val settings = SettingsStore(context).settings.first()
        val governorate = GovernoratesData.byId(settings.governorateId)

        cancelAll(context)

        val alertTimingMinutes = settings.prayerAlertTiming

        for (dayOffset in 0..1) {
            val day = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, dayOffset)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val schedule = EgyptPrayerEngine.calculate(day, governorate.lat, governorate.lng)

            PrayerKey.prayers.forEach { key ->
                if (key == PrayerKey.FAJR && !settings.fajrAlertEnabled) return@forEach

                val offsetMs = settings.prayerOffsets.minutesFor(key) * MINUTE_MS
                val adhanAt = schedule.millisFor(key) + offsetMs

                when (settings.modeFor(key)) {
                    com.ahmoodiy.prayer.domain.model.PrayerNotificationMode.MUTE -> return@forEach

                    com.ahmoodiy.prayer.domain.model.PrayerNotificationMode.ADHAN -> {
                        schedule(context, key.id, adhanAt, isPreReminder = false)
                        if (alertTimingMinutes > 0) {
                            schedule(
                                context,
                                key.id,
                                adhanAt - alertTimingMinutes * MINUTE_MS,
                                isPreReminder = true,
                            )
                        }
                    }

                    com.ahmoodiy.prayer.domain.model.PrayerNotificationMode.PRE_REMINDER -> {
                        if (alertTimingMinutes > 0) {
                            schedule(
                                context,
                                key.id,
                                adhanAt - alertTimingMinutes * MINUTE_MS,
                                isPreReminder = true,
                            )
                        }
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------- internals

    private fun buildPendingIntent(
        context: Context,
        prayerKey: String,
        isPreReminder: Boolean,
    ): PendingIntent {
        val intent = Intent(context, AdhanReceiver::class.java).apply {
            action = ACTION_PRAYER_ALARM
            putExtra(AdhanReceiver.EXTRA_PRAYER_KEY, prayerKey)
            putExtra(AdhanReceiver.EXTRA_IS_PRE_REMINDER, isPreReminder)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode(prayerKey, isPreReminder), intent, flags)
    }

    private fun requestCode(prayerKey: String, isPreReminder: Boolean): Int =
        prayerKey.hashCode() * 2 + if (isPreReminder) 1 else 0
}
