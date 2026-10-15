package com.ahmoodiy.prayer.domain.model

/** Per-prayer manual offset in minutes (clamped to ±10 in the UI). */
data class PrayerOffsets(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0,
) {
    fun minutesFor(key: PrayerKey): Int = when (key) {
        PrayerKey.FAJR -> fajr
        PrayerKey.SUNRISE -> sunrise
        PrayerKey.DHUHR -> dhuhr
        PrayerKey.ASR -> asr
        PrayerKey.MAGHRIB -> maghrib
        PrayerKey.ISHA -> isha
    }

    fun updated(key: PrayerKey, minutes: Int): PrayerOffsets {
        val clamped = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)
        return when (key) {
            PrayerKey.FAJR -> copy(fajr = clamped)
            PrayerKey.SUNRISE -> copy(sunrise = clamped)
            PrayerKey.DHUHR -> copy(dhuhr = clamped)
            PrayerKey.ASR -> copy(asr = clamped)
            PrayerKey.MAGHRIB -> copy(maghrib = clamped)
            PrayerKey.ISHA -> copy(isha = clamped)
        }
    }

    companion object {
        const val MIN_MINUTES = -10
        const val MAX_MINUTES = 10
    }
}

/** How the app should alert for a given prayer. */
enum class PrayerNotificationMode(val id: String) {
    ADHAN("sound"),
    PRE_REMINDER("pre_15"),
    MUTE("mute");

    fun next(): PrayerNotificationMode = when (this) {
        ADHAN -> PRE_REMINDER
        PRE_REMINDER -> MUTE
        MUTE -> ADHAN
    }

    companion object {
        fun fromId(id: String?): PrayerNotificationMode =
            entries.firstOrNull { it.id.equals(id, true) } ?: ADHAN
    }
}

/** Tombstone for the user-picked local adhan file. */
data class CustomAudioMeta(
    val name: String,
    val size: Long,
    val type: String,
    val durationMs: Long?,
    val addedAtEpochMs: Long,
)
