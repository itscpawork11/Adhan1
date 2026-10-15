package com.ahmoodiy.prayer.util

import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Arabic-friendly time formatting helpers (Western digits, ص/م markers). */
object ArabicTime {

    /** Formats an instant as `hh:mm ص|م` using the device time zone. */
    fun formatTime(millis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        var hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val isPm = hour >= 12
        hour %= 12
        if (hour == 0) hour = 12
        return "%02d:%02d %s".format(hour, minute, if (isPm) "م" else "ص")
    }

    /** Splits the remaining time into zero-padded hours / minutes / seconds. */
    data class Countdown(val hours: String, val minutes: String, val seconds: String, val totalSeconds: Long) {
        val isExpired: Boolean get() = totalSeconds <= 0

        /** Compact `HH:MM:SS` used by the hero card. */
        val compact: String get() = "$hours:$minutes:$seconds"
    }

    fun countdownUntil(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): Countdown {
        val diff = targetMillis - nowMillis
        if (diff <= 0) return Countdown("00", "00", "00", 0)
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return Countdown(
            hours = pad(hours),
            minutes = pad(minutes),
            seconds = pad(seconds),
            totalSeconds = totalSeconds,
        )
    }

    private fun pad(value: Long): String = if (value < 10) "0$value" else value.toString()
}
