package com.ahmoodiy.prayer.domain.model

/**
 * The six scheduled moments shown on the prayer timetable.
 * [sunrise] is a time marker, not a prayer, and never gets an adhan.
 */
enum class PrayerKey(
    val id: String,
    val arabicName: String,
    val englishName: String,
    val isPrayer: Boolean,
) {
    FAJR("fajr", "الفجر", "Fajr", true),
    SUNRISE("sunrise", "الشروق", "Sunrise", false),
    DHUHR("dhuhr", "الظهر", "Dhuhr", true),
    ASR("asr", "العصر", "Asr", true),
    MAGHRIB("maghrib", "المغرب", "Maghrib", true),
    ISHA("isha", "العشاء", "Isha", true);

    companion object {
        val prayers: List<PrayerKey> = entries.filter { it.isPrayer }
        val ordered: List<PrayerKey> = entries.toList()

        fun fromId(id: String?): PrayerKey? = entries.firstOrNull { it.id.equals(id, true) }
    }
}
