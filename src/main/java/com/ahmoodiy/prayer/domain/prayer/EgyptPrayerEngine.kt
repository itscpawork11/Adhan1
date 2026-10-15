package com.ahmoodiy.prayer.domain.prayer

import com.ahmoodiy.prayer.domain.model.PrayerKey
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * Offline prayer-time engine implementing the Egyptian General Authority of Survey method.
 *
 * The math is a faithful port of the original web implementation, including the
 * Egyptian Daylight Saving Time window (last Friday of April until the last Thursday of October).
 */
object EgyptPrayerEngine {

    private const val FAJR_TWILIGHT_ANGLE = 19.5
    private const val ISHA_TWILIGHT_ANGLE = 17.5
    private const val SUN_REFRACTION_ANGLE = -0.833

    /** Milliseconds since epoch for each of the six daily moments. */
    data class PrayerSchedule(
        val fajr: Long,
        val sunrise: Long,
        val dhuhr: Long,
        val asr: Long,
        val maghrib: Long,
        val isha: Long,
    ) {
        fun millisFor(key: PrayerKey): Long = when (key) {
            PrayerKey.FAJR -> fajr
            PrayerKey.SUNRISE -> sunrise
            PrayerKey.DHUHR -> dhuhr
            PrayerKey.ASR -> asr
            PrayerKey.MAGHRIB -> maghrib
            PrayerKey.ISHA -> isha
        }

        fun asMap(): Map<PrayerKey, Long> = PrayerKey.ordered.associateWith { millisFor(it) }
    }

    fun calculate(date: Calendar, lat: Double, lng: Double): PrayerSchedule {
        val dayOfYear = date.get(Calendar.DAY_OF_YEAR)
        val gamma = (2.0 * Math.PI / 365.0) * (dayOfYear - 1)

        val eqTime = 229.18 * (
            0.000075 +
                0.001868 * cos(gamma) -
                0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) -
                0.040849 * sin(2 * gamma)
            )

        val decl = 0.006918 -
            0.399912 * cos(gamma) +
            0.070257 * sin(gamma) -
            0.006758 * cos(2 * gamma) +
            0.000907 * sin(2 * gamma) -
            0.002697 * cos(3 * gamma) +
            0.00148 * sin(3 * gamma)

        val timezoneOffsetHours = if (isEgyptianDst(date)) 3.0 else 2.0

        val solarNoonUtcMinutes = 720.0 - (4.0 * lng) - eqTime
        val solarNoonLocalHours = (solarNoonUtcMinutes / 60.0) + timezoneOffsetHours
        val dhuhrHours = solarNoonLocalHours + (1.5 / 60.0)

        val latRad = Math.toRadians(lat)

        fun hourAngle(angleDeg: Double, isBelow: Boolean): Double {
            val h = if (isBelow) Math.toRadians(-angleDeg) else Math.toRadians(angleDeg)
            val cosH = (sin(h) - sin(latRad) * sin(decl)) / (cos(latRad) * cos(decl))
            val clamped = cosH.coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(clamped)) / 15.0
        }

        val sunriseSunsetHa = hourAngle(SUN_REFRACTION_ANGLE, false)
        val sunriseHours = solarNoonLocalHours - sunriseSunsetHa
        val maghribHours = solarNoonLocalHours + sunriseSunsetHa

        val fajrHa = hourAngle(FAJR_TWILIGHT_ANGLE, true)
        val fajrHours = solarNoonLocalHours - fajrHa

        val ishaHa = hourAngle(ISHA_TWILIGHT_ANGLE, true)
        val ishaHours = solarNoonLocalHours + ishaHa

        // Shafi'i / Maliki / Hanbali shadow factor of 1.
        val asrAlt = atan(1.0 / (1.0 + tan(abs(latRad - decl))))
        val cosAsr = (sin(asrAlt) - sin(latRad) * sin(decl)) / (cos(latRad) * cos(decl))
        val asrHa = Math.toDegrees(acos(cosAsr.coerceIn(-1.0, 1.0))) / 15.0
        val asrHours = solarNoonLocalHours + asrHa

        return PrayerSchedule(
            fajr = toTimestamp(date, fajrHours),
            sunrise = toTimestamp(date, sunriseHours),
            dhuhr = toTimestamp(date, dhuhrHours),
            asr = toTimestamp(date, asrHours),
            maghrib = toTimestamp(date, maghribHours),
            isha = toTimestamp(date, ishaHours),
        )
    }

    /** Convenience overload that runs the calculation for "today". */
    fun calculateToday(lat: Double, lng: Double): PrayerSchedule =
        calculate(Calendar.getInstance(), lat, lng)

    /**
     * Egyptian DST: from the last Friday of April to the last Thursday of October.
     * Mirrors the original `Date#getMonth` (0-based) and `Date#getDay` semantics.
     */
    internal fun isEgyptianDst(date: Calendar): Boolean {
        val month = date.get(Calendar.MONTH) // 0-based, matching JS
        return when {
            month > 3 && month < 9 -> true
            month == 3 -> date.get(Calendar.DAY_OF_MONTH) >= 25 &&
                date.get(Calendar.DAY_OF_WEEK) >= Calendar.FRIDAY
            month == 9 -> date.get(Calendar.DAY_OF_MONTH) < 25
            else -> false
        }
    }

    private fun toTimestamp(date: Calendar, hoursDecimal: Double): Long {
        var normalized = hoursDecimal % 24.0
        if (normalized < 0) normalized += 24.0
        val hours = floor(normalized).toInt()
        val minutesDecimal = (normalized - hours) * 60.0
        val minutes = floor(minutesDecimal).toInt()
        val seconds = ((minutesDecimal - minutes) * 60.0).roundToInt()

        return (date.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hours)
            set(Calendar.MINUTE, minutes)
            set(Calendar.SECOND, seconds)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
