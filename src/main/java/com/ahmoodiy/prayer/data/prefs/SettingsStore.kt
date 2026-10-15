package com.ahmoodiy.prayer.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ahmoodiy.prayer.domain.model.AdhanVoice
import com.ahmoodiy.prayer.domain.model.AppSettings
import com.ahmoodiy.prayer.domain.model.ArabicFont
import com.ahmoodiy.prayer.domain.model.CustomAudioMeta
import com.ahmoodiy.prayer.domain.model.FontScale
import com.ahmoodiy.prayer.domain.model.PrayerKey
import com.ahmoodiy.prayer.domain.model.PrayerNotificationMode
import com.ahmoodiy.prayer.domain.model.PrayerOffsets
import com.ahmoodiy.prayer.domain.model.TasbeehSoundType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Persists [AppSettings] with DataStore Preferences. */
class SettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs -> prefs.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs.write(updated)
        }
    }

    suspend fun set(settings: AppSettings) {
        context.settingsDataStore.edit { prefs -> prefs.write(settings) }
    }

    private fun Preferences.toSettings() = AppSettings(
        font = ArabicFont.fromId(this[Keys.FONT]),
        fontScale = FontScale.fromId(this[Keys.FONT_SCALE]),
        governorateId = this[Keys.GOVERNORATE] ?: "cairo",
        prayerOffsets = PrayerOffsets(
            fajr = this[Keys.OFFSET_FAJR] ?: 0,
            sunrise = this[Keys.OFFSET_SUNRISE] ?: 0,
            dhuhr = this[Keys.OFFSET_DHUHR] ?: 0,
            asr = this[Keys.OFFSET_ASR] ?: 0,
            maghrib = this[Keys.OFFSET_MAGHRIB] ?: 0,
            isha = this[Keys.OFFSET_ISHA] ?: 0,
        ),
        prayerAlertTiming = this[Keys.ALERT_TIMING] ?: 10,
        prayerModes = decodeModes(this[Keys.PRAYER_MODES]),
        adhanVoice = AdhanVoice.fromId(this[Keys.ADHAN_VOICE]),
        customAudioMeta = readCustomAudio(),
        fajrAlertEnabled = this[Keys.FAJR_ALERT] ?: true,
        tasbeehHapticOnTap = this[Keys.HAPTIC_TAP] ?: true,
        tasbeehLongHapticOnCycle = this[Keys.HAPTIC_CYCLE] ?: true,
        tasbeehSoundEnabled = this[Keys.TASBEEH_SOUND_ENABLED] ?: true,
        tasbeehSoundType = TasbeehSoundType.fromId(this[Keys.TASBEEH_SOUND_TYPE]),
        tasbeehResetProtection = this[Keys.RESET_PROTECTION] ?: true,
        tasbeehDefaultTarget = this[Keys.DEFAULT_TARGET] ?: 33,
        showTashkeel = this[Keys.SHOW_TASHKEEL] ?: true,
        showSanadAndVirtue = this[Keys.SHOW_SANAD] ?: true,
        autoHideCompletedThikr = this[Keys.AUTO_HIDE_COMPLETED] ?: false,
        dailyReminderEnabled = this[Keys.DAILY_REMINDER] ?: true,
        morningReminderTime = this[Keys.MORNING_REMINDER] ?: "06:00",
        eveningReminderTime = this[Keys.EVENING_REMINDER] ?: "17:00",
        shareFormatWithAppName = this[Keys.SHARE_APP_NAME] ?: true,
    )

    private fun Preferences.readCustomAudio(): CustomAudioMeta? {
        val name = this[Keys.CUSTOM_AUDIO_NAME] ?: return null
        return CustomAudioMeta(
            name = name,
            size = this[Keys.CUSTOM_AUDIO_SIZE] ?: 0L,
            type = this[Keys.CUSTOM_AUDIO_TYPE] ?: "audio/mpeg",
            durationMs = this[Keys.CUSTOM_AUDIO_DURATION],
            addedAtEpochMs = this[Keys.CUSTOM_AUDIO_ADDED_AT] ?: 0L,
        )
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.write(settings: AppSettings) {
        this[Keys.FONT] = settings.font.id
        this[Keys.FONT_SCALE] = settings.fontScale.id
        this[Keys.GOVERNORATE] = settings.governorateId
        this[Keys.OFFSET_FAJR] = settings.prayerOffsets.fajr
        this[Keys.OFFSET_SUNRISE] = settings.prayerOffsets.sunrise
        this[Keys.OFFSET_DHUHR] = settings.prayerOffsets.dhuhr
        this[Keys.OFFSET_ASR] = settings.prayerOffsets.asr
        this[Keys.OFFSET_MAGHRIB] = settings.prayerOffsets.maghrib
        this[Keys.OFFSET_ISHA] = settings.prayerOffsets.isha
        this[Keys.ALERT_TIMING] = settings.prayerAlertTiming
        this[Keys.PRAYER_MODES] = encodeModes(settings.prayerModes)
        this[Keys.ADHAN_VOICE] = settings.adhanVoice.id
        this[Keys.FAJR_ALERT] = settings.fajrAlertEnabled
        this[Keys.HAPTIC_TAP] = settings.tasbeehHapticOnTap
        this[Keys.HAPTIC_CYCLE] = settings.tasbeehLongHapticOnCycle
        this[Keys.TASBEEH_SOUND_ENABLED] = settings.tasbeehSoundEnabled
        this[Keys.TASBEEH_SOUND_TYPE] = settings.tasbeehSoundType.id
        this[Keys.RESET_PROTECTION] = settings.tasbeehResetProtection
        this[Keys.DEFAULT_TARGET] = settings.tasbeehDefaultTarget
        this[Keys.SHOW_TASHKEEL] = settings.showTashkeel
        this[Keys.SHOW_SANAD] = settings.showSanadAndVirtue
        this[Keys.AUTO_HIDE_COMPLETED] = settings.autoHideCompletedThikr
        this[Keys.DAILY_REMINDER] = settings.dailyReminderEnabled
        this[Keys.MORNING_REMINDER] = settings.morningReminderTime
        this[Keys.EVENING_REMINDER] = settings.eveningReminderTime
        this[Keys.SHARE_APP_NAME] = settings.shareFormatWithAppName

        val meta = settings.customAudioMeta
        if (meta == null) {
            this.remove(Keys.CUSTOM_AUDIO_NAME)
            this.remove(Keys.CUSTOM_AUDIO_SIZE)
            this.remove(Keys.CUSTOM_AUDIO_TYPE)
            this.remove(Keys.CUSTOM_AUDIO_DURATION)
            this.remove(Keys.CUSTOM_AUDIO_ADDED_AT)
        } else {
            this[Keys.CUSTOM_AUDIO_NAME] = meta.name
            this[Keys.CUSTOM_AUDIO_SIZE] = meta.size
            this[Keys.CUSTOM_AUDIO_TYPE] = meta.type
            meta.durationMs?.let { this[Keys.CUSTOM_AUDIO_DURATION] = it }
            this[Keys.CUSTOM_AUDIO_ADDED_AT] = meta.addedAtEpochMs
        }
    }

    /** Encodes the per-prayer alert modes as `fajr:sound,dhuhr:mute,...`. */
    private fun encodeModes(modes: Map<PrayerKey, PrayerNotificationMode>): String =
        modes.entries.joinToString(",") { "${it.key.id}:${it.value.id}" }

    private fun decodeModes(raw: String?): Map<PrayerKey, PrayerNotificationMode> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val key = PrayerKey.fromId(parts[0].trim()) ?: return@mapNotNull null
            key to PrayerNotificationMode.fromId(parts[1].trim())
        }.toMap()
    }

    private object Keys {
        val FONT = stringPreferencesKey("font_family")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val GOVERNORATE = stringPreferencesKey("governorate_id")
        val OFFSET_FAJR = intPreferencesKey("offset_fajr")
        val OFFSET_SUNRISE = intPreferencesKey("offset_sunrise")
        val OFFSET_DHUHR = intPreferencesKey("offset_dhuhr")
        val OFFSET_ASR = intPreferencesKey("offset_asr")
        val OFFSET_MAGHRIB = intPreferencesKey("offset_maghrib")
        val OFFSET_ISHA = intPreferencesKey("offset_isha")
        val ALERT_TIMING = intPreferencesKey("alert_timing_minutes")
        val PRAYER_MODES = stringPreferencesKey("prayer_modes")
        val ADHAN_VOICE = stringPreferencesKey("adhan_voice")
        val FAJR_ALERT = booleanPreferencesKey("fajr_alert_enabled")
        val HAPTIC_TAP = booleanPreferencesKey("haptic_on_tap")
        val HAPTIC_CYCLE = booleanPreferencesKey("haptic_on_cycle")
        val TASBEEH_SOUND_ENABLED = booleanPreferencesKey("tasbeeh_sound_enabled")
        val TASBEEH_SOUND_TYPE = stringPreferencesKey("tasbeeh_sound_type")
        val RESET_PROTECTION = booleanPreferencesKey("reset_protection")
        val DEFAULT_TARGET = intPreferencesKey("tasbeeh_default_target")
        val SHOW_TASHKEEL = booleanPreferencesKey("show_tashkeel")
        val SHOW_SANAD = booleanPreferencesKey("show_sanad_and_virtue")
        val AUTO_HIDE_COMPLETED = booleanPreferencesKey("auto_hide_completed")
        val DAILY_REMINDER = booleanPreferencesKey("daily_reminder_enabled")
        val MORNING_REMINDER = stringPreferencesKey("morning_reminder_time")
        val EVENING_REMINDER = stringPreferencesKey("evening_reminder_time")
        val SHARE_APP_NAME = booleanPreferencesKey("share_with_app_name")
        val CUSTOM_AUDIO_NAME = stringPreferencesKey("custom_audio_name")
        val CUSTOM_AUDIO_SIZE = longPreferencesKey("custom_audio_size")
        val CUSTOM_AUDIO_TYPE = stringPreferencesKey("custom_audio_type")
        val CUSTOM_AUDIO_DURATION = longPreferencesKey("custom_audio_duration")
        val CUSTOM_AUDIO_ADDED_AT = longPreferencesKey("custom_audio_added_at")
    }
}
