package com.ahmoodiy.prayer.domain.model

/** The complete, persisted user configuration. Mirrors the legacy web settings object. */
data class AppSettings(
    // --- Typography ---
    val font: ArabicFont = ArabicFont.AMIRI,
    val fontScale: FontScale = FontScale.BASE,

    // --- Prayer times ---
    val governorateId: String = "cairo",
    val prayerOffsets: PrayerOffsets = PrayerOffsets(),
    val prayerAlertTiming: Int = 10, // minutes before the adhan for the pre-reminder
    /**
     * Per-prayer alert mode. A missing key means "play the adhan" (the default).
     */
    val prayerModes: Map<PrayerKey, PrayerNotificationMode> = emptyMap(),
    val adhanVoice: AdhanVoice = AdhanVoice.ABDULBASIT,
    val customAudioMeta: CustomAudioMeta? = null,
    val fajrAlertEnabled: Boolean = true,

    // --- Tasbeeh ---
    val tasbeehHapticOnTap: Boolean = true,
    val tasbeehLongHapticOnCycle: Boolean = true,
    val tasbeehSoundEnabled: Boolean = true,
    val tasbeehSoundType: TasbeehSoundType = TasbeehSoundType.WOOD,
    val tasbeehResetProtection: Boolean = true,
    val tasbeehDefaultTarget: Int = 33,

    // --- Athkar ---
    val showTashkeel: Boolean = true,
    val showSanadAndVirtue: Boolean = true,
    val autoHideCompletedThikr: Boolean = false,
    val dailyReminderEnabled: Boolean = true,
    val morningReminderTime: String = "06:00",
    val eveningReminderTime: String = "17:00",

    // --- Duas ---
    val shareFormatWithAppName: Boolean = true,
) {
    /** Effective alert mode for a prayer (defaults to the adhan). */
    fun modeFor(key: PrayerKey): PrayerNotificationMode = prayerModes[key] ?: PrayerNotificationMode.ADHAN
}

/** Rosary tap sound. */
enum class TasbeehSoundType(val id: String, val displayName: String) {
    WOOD("wood", "خشب طبيعي"),
    CRYSTAL("crystal", "رنين كريستال"),
    MUTE("mute", "صامت تماماً");

    companion object {
        val ordered: List<TasbeehSoundType> = listOf(WOOD, CRYSTAL, MUTE)

        fun fromId(id: String?): TasbeehSoundType = entries.firstOrNull { it.id.equals(id, true) } ?: WOOD
    }
}

/** Aggregated rosary statistics shown by the metrics panel and Settings. */
data class TasbeehStats(
    val currentCount: Int = 0,
    val dailyTotal: Int = 0,
    val lifetimeTotal: Int = 0,
    val cyclesCompleted: Int = 0,
    /** ISO date (yyyy-MM-dd) the [dailyTotal] belongs to, used to roll the counter over. */
    val dailyDate: String = "",
)
