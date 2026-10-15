package com.ahmoodiy.prayer.util

import android.content.Context
import android.net.Uri
import com.ahmoodiy.prayer.data.prefs.SettingsStore
import com.ahmoodiy.prayer.data.prefs.UserDataStore
import com.ahmoodiy.prayer.domain.model.AdhanVoice
import com.ahmoodiy.prayer.domain.model.AppSettings
import com.ahmoodiy.prayer.domain.model.ArabicFont
import com.ahmoodiy.prayer.domain.model.CustomAudioMeta
import com.ahmoodiy.prayer.domain.model.FontScale
import com.ahmoodiy.prayer.domain.model.PrayerKey
import com.ahmoodiy.prayer.domain.model.PrayerNotificationMode
import com.ahmoodiy.prayer.domain.model.PrayerOffsets
import com.ahmoodiy.prayer.domain.model.TasbeehPhrase
import com.ahmoodiy.prayer.domain.model.TasbeehSoundType
import com.ahmoodiy.prayer.domain.model.TasbeehStats
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON backup / restore over the Storage Access Framework.
 *
 * The payload is versioned so future builds can migrate older files instead of failing.
 */
class BackupManager(
    private val context: Context,
    private val settingsStore: SettingsStore,
    private val userDataStore: UserDataStore,
) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    /** Serialises the entire user state. */
    suspend fun exportToString(): String {
        val settings = settingsStore.settings.first()
        val stats = userDataStore.stats.first()
        val favorites = userDataStore.favorites.first()
        val counters = userDataStore.counters.first()
        val phrases = userDataStore.customPhrases.first()

        return json.encodeToString(
            BackupDoc(
                version = SCHEMA_VERSION,
                exportedAt = System.currentTimeMillis(),
                settings = settings.toDoc(),
                stats = stats.toDoc(),
                favorites = favorites.toList(),
                counters = counters.mapKeys { it.key.toString() },
                customPhrases = phrases.map { it.toDoc() },
            ),
        )
    }

    /** Writes the backup to a SAF document. Returns true on success. */
    fun writeToUri(uri: Uri, content: String): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("Cannot open output stream")
        true
    }.getOrDefault(false)

    /** Reads a backup document. Returns null when the file is not readable / not valid JSON. */
    fun readFromUri(uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        }
    }.getOrNull()

    /** Applies a previously exported backup. Returns a human readable result message. */
    suspend fun restoreFromString(content: String): RestoreResult {
        val doc = runCatching { json.decodeFromString<BackupDoc>(content) }
            .getOrElse { return RestoreResult(false, "الملف غير صالح أو تالف") }

        if (doc.version > SCHEMA_VERSION) {
            return RestoreResult(false, "النسخة الاحتياطية من إصدار أحدث من تطبيقك")
        }

        runCatching {
            settingsStore.set(doc.settings.toModel())
            userDataStore.setStats(doc.stats.toModel())
            userDataStore.setFavorites(doc.favorites.toSet())
            userDataStore.setCounters(doc.counters.mapNotNull { (key, value) ->
                key.toIntOrNull()?.let { it to value }
            }.toMap())
            userDataStore.setCustomPhrases(doc.customPhrases.map { it.toModel() })
        }.onFailure {
            return RestoreResult(false, "تعذر تطبيق النسخة الاحتياطية")
        }

        return RestoreResult(
            success = true,
            message = "تمت الاستعادة بنجاح · ${doc.stats.lifetimeTotal} تسبيحة",
        )
    }

    // ---------------------------------------------------------------- DTOs

    @Serializable
    data class RestoreResult(val success: Boolean, val message: String)

    @Serializable
    data class BackupDoc(
        val version: Int = SCHEMA_VERSION,
        val exportedAt: Long = 0L,
        val app: String = "com.ahmoodiy.prayer",
        val settings: SettingsDoc = SettingsDoc(),
        val stats: StatsDoc = StatsDoc(),
        val favorites: List<Int> = emptyList(),
        val counters: Map<String, Int> = emptyMap(),
        val customPhrases: List<PhraseDoc> = emptyList(),
    )

    @Serializable
    data class SettingsDoc(
        val font: String = ArabicFont.AMIRI.id,
        val fontScale: String = FontScale.BASE.id,
        val governorateId: String = "cairo",
        val offsetFajr: Int = 0,
        val offsetSunrise: Int = 0,
        val offsetDhuhr: Int = 0,
        val offsetAsr: Int = 0,
        val offsetMaghrib: Int = 0,
        val offsetIsha: Int = 0,
        val alertTiming: Int = 10,
        val prayerModes: Map<String, String> = emptyMap(),
        val adhanVoice: String = AdhanVoice.ABDULBASIT.id,
        val fajrAlertEnabled: Boolean = true,
        val hapticTap: Boolean = true,
        val hapticCycle: Boolean = true,
        val tasbeehSoundEnabled: Boolean = true,
        val tasbeehSoundType: String = TasbeehSoundType.WOOD.id,
        val resetProtection: Boolean = true,
        val defaultTarget: Int = 33,
        val showTashkeel: Boolean = true,
        val showSanadAndVirtue: Boolean = true,
        val autoHideCompletedThikr: Boolean = false,
        val dailyReminderEnabled: Boolean = true,
        val morningReminderTime: String = "06:00",
        val eveningReminderTime: String = "17:00",
        val shareWithAppName: Boolean = true,
    )

    @Serializable
    data class StatsDoc(
        val currentCount: Int = 0,
        val dailyTotal: Int = 0,
        val lifetimeTotal: Int = 0,
        val cyclesCompleted: Int = 0,
        val dailyDate: String = "",
    )

    @Serializable
    data class PhraseDoc(
        val id: String = "",
        val arabicText: String = "",
        val virtue: String = "",
        val defaultTarget: Int = 33,
    )

    private fun AppSettings.toDoc() = SettingsDoc(
        font = font.id,
        fontScale = fontScale.id,
        governorateId = governorateId,
        offsetFajr = prayerOffsets.fajr,
        offsetSunrise = prayerOffsets.sunrise,
        offsetDhuhr = prayerOffsets.dhuhr,
        offsetAsr = prayerOffsets.asr,
        offsetMaghrib = prayerOffsets.maghrib,
        offsetIsha = prayerOffsets.isha,
        alertTiming = prayerAlertTiming,
        prayerModes = prayerModes.mapKeys { it.key.id }.mapValues { it.value.id },
        adhanVoice = adhanVoice.id,
        fajrAlertEnabled = fajrAlertEnabled,
        hapticTap = tasbeehHapticOnTap,
        hapticCycle = tasbeehLongHapticOnCycle,
        tasbeehSoundEnabled = tasbeehSoundEnabled,
        tasbeehSoundType = tasbeehSoundType.id,
        resetProtection = tasbeehResetProtection,
        defaultTarget = tasbeehDefaultTarget,
        showTashkeel = showTashkeel,
        showSanadAndVirtue = showSanadAndVirtue,
        autoHideCompletedThikr = autoHideCompletedThikr,
        dailyReminderEnabled = dailyReminderEnabled,
        morningReminderTime = morningReminderTime,
        eveningReminderTime = eveningReminderTime,
        shareWithAppName = shareFormatWithAppName,
    )

    private fun SettingsDoc.toModel() = AppSettings(
        font = ArabicFont.fromId(font),
        fontScale = FontScale.fromId(fontScale),
        governorateId = governorateId,
        prayerOffsets = PrayerOffsets(offsetFajr, offsetSunrise, offsetDhuhr, offsetAsr, offsetMaghrib, offsetIsha),
        prayerAlertTiming = alertTiming,
        prayerModes = prayerModes.mapNotNull { (key, value) ->
            PrayerKey.fromId(key)?.let { it to PrayerNotificationMode.fromId(value) }
        }.toMap(),
        adhanVoice = AdhanVoice.fromId(adhanVoice),
        customAudioMeta = null, // the audio file itself is app-private and never travels in JSON
        fajrAlertEnabled = fajrAlertEnabled,
        tasbeehHapticOnTap = hapticTap,
        tasbeehLongHapticOnCycle = hapticCycle,
        tasbeehSoundEnabled = tasbeehSoundEnabled,
        tasbeehSoundType = TasbeehSoundType.fromId(tasbeehSoundType),
        tasbeehResetProtection = resetProtection,
        tasbeehDefaultTarget = defaultTarget,
        showTashkeel = showTashkeel,
        showSanadAndVirtue = showSanadAndVirtue,
        autoHideCompletedThikr = autoHideCompletedThikr,
        dailyReminderEnabled = dailyReminderEnabled,
        morningReminderTime = morningReminderTime,
        eveningReminderTime = eveningReminderTime,
        shareFormatWithAppName = shareWithAppName,
    )

    private fun TasbeehStats.toDoc() = StatsDoc(
        currentCount = currentCount,
        dailyTotal = dailyTotal,
        lifetimeTotal = lifetimeTotal,
        cyclesCompleted = cyclesCompleted,
        dailyDate = dailyDate,
    )

    private fun StatsDoc.toModel() = TasbeehStats(
        currentCount = currentCount,
        dailyTotal = dailyTotal,
        lifetimeTotal = lifetimeTotal,
        cyclesCompleted = cyclesCompleted,
        dailyDate = dailyDate,
    )

    private fun TasbeehPhrase.toDoc() = PhraseDoc(id, arabicText, virtue, defaultTarget)
    private fun PhraseDoc.toModel() = TasbeehPhrase(id, arabicText, virtue, defaultTarget, isCustom = true)

    companion object {
        const val SCHEMA_VERSION = 1
        const val FILE_NAME = "ahmoodiy-prayer-backup.json"
    }
}
