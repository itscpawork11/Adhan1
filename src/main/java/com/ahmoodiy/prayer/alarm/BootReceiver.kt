package com.ahmoodiy.prayer.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-schedules prayer alarms after reboot, time/timezone changes and app updates so a reboot
 * can never silently disable the adhan.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action !in TRIGGER_ACTIONS) return

        // BroadcastReceiver has only a few seconds; goAsync keeps the process alive while
        // we recompute the timetable and re-register every alarm.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AdhanAlarmScheduler.rescheduleAll(context)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val TRIGGER_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
