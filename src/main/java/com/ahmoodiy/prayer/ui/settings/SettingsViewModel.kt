package com.ahmoodiy.prayer.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmoodiy.prayer.alarm.AdhanAlarmScheduler
import com.ahmoodiy.prayer.di.AppContainer
import com.ahmoodiy.prayer.di.containerViewModelFactory
import com.ahmoodiy.prayer.domain.model.AdhanVoice
import com.ahmoodiy.prayer.domain.model.AppSettings
import com.ahmoodiy.prayer.domain.model.ArabicFont
import com.ahmoodiy.prayer.domain.model.FontScale
import com.ahmoodiy.prayer.domain.model.TasbeehSoundType
import com.ahmoodiy.prayer.domain.model.TasbeehStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Everything the Settings screen renders.
 *
 * [settings] is the single source of truth (observed from [com.ahmoodiy.prayer.data.prefs.SettingsStore]),
 * while the remaining fields are transient UI flags owned by the ViewModel.
 */
data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    /** Non-null while an adhan preview is playing. */
    val playingVoice: AdhanVoice? = null,
    /** A one-shot result / info message surfaced through a dialog. */
    val message: String? = null,
    /** When true the [message] describes a failure and the dialog title reflects it. */
    val messageIsError: Boolean = false,
    /** Controls the "erase everything" confirmation dialog. */
    val showResetDialog: Boolean = false,
)

/**
 * Drives the Settings screen.
 *
 * Every mutation is written through [com.ahmoodiy.prayer.data.prefs.SettingsStore.update] using the
 * atomic read-modify-write transform style, which avoids the races a read-from-flow + write cycle
 * would introduce. Changes that affect alarm scheduling (governorate, offsets, modes, alert timing,
 * fajr alert or adhan voice) additionally call [AdhanAlarmScheduler.rescheduleAll].
 *
 * The display name / MIME type for an imported custom adhan are resolved by the screen (through the
 * Storage Access Framework + [android.provider.OpenableColumns]) and passed in as plain values, so
 * this ViewModel never has to touch the [android.content.ContentResolver] for that query.
 */
class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val settingsStore = container.settingsStore
    private val userDataStore = container.userDataStore
    private val beadSoundPlayer = container.beadSoundPlayer
    private val adhanPlayer = container.adhanPlayer
    private val customAdhanStore = container.customAdhanStore
    private val backupManager = container.backupManager

    private val localState = MutableStateFlow(SettingsUiState())

    /** Live settings merged with the transient UI flags owned by this ViewModel. */
    val state: StateFlow<SettingsUiState> = combine(
        settingsStore.settings,
        localState,
    ) { settings, local ->
        local.copy(settings = settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    // ------------------------------------------------------------------ typography

    /** Persists the selected Arabic typeface. */
    fun setFont(font: ArabicFont) = persist { it.copy(font = font) }

    /** Persists the reader font-size scale. */
    fun setFontScale(scale: FontScale) = persist { it.copy(fontScale = scale) }

    fun setShowTashkeel(enabled: Boolean) = persist { it.copy(showTashkeel = enabled) }

    fun setShowSanadAndVirtue(enabled: Boolean) = persist { it.copy(showSanadAndVirtue = enabled) }

    fun setAutoHideCompletedThikr(enabled: Boolean) = persist { it.copy(autoHideCompletedThikr = enabled) }

    // ------------------------------------------------------------------ rosary

    fun setHapticOnTap(enabled: Boolean) = persist { it.copy(tasbeehHapticOnTap = enabled) }

    fun setLongHapticOnCycle(enabled: Boolean) = persist { it.copy(tasbeehLongHapticOnCycle = enabled) }

    fun setSoundEnabled(enabled: Boolean) = persist { it.copy(tasbeehSoundEnabled = enabled) }

    /** Persists the bead tap sound and previews it (unless the choice is silent). */
    fun setSoundType(type: TasbeehSoundType) {
        if (type != TasbeehSoundType.MUTE) beadSoundPlayer.play(type)
        persist { it.copy(tasbeehSoundType = type) }
    }

    fun setResetProtection(enabled: Boolean) = persist { it.copy(tasbeehResetProtection = enabled) }

    fun setDefaultTarget(target: Int) = persist { it.copy(tasbeehDefaultTarget = target) }

    // ------------------------------------------------------------------ alerts & adhan

    /** Toggles the fajr alert and re-registers the prayer alarms. */
    fun setFajrAlertEnabled(enabled: Boolean) =
        persist(reschedule = true) { it.copy(fajrAlertEnabled = enabled) }

    /** Selects a governorate and re-registers the prayer alarms. */
    fun setGovernorate(governorateId: String) =
        persist(reschedule = true) { it.copy(governorateId = governorateId) }

    /** Sets the pre-adhan reminder window (clamped to 0..30 minutes) and re-registers alarms. */
    fun setAlertTiming(minutes: Int) =
        persist(reschedule = true) { it.copy(prayerAlertTiming = minutes.coerceIn(MIN_ALERT_MINUTES, MAX_ALERT_MINUTES)) }

    /** Persists the adhan voice and re-registers the prayer alarms. */
    fun selectVoice(voice: AdhanVoice) = persist(reschedule = true) { it.copy(adhanVoice = voice) }

    /**
     * Starts or stops an inline preview of [voice].
     * The user's imported file is passed for [AdhanVoice.CUSTOM]; other voices are synthesised.
     */
    fun toggleVoicePreview(voice: AdhanVoice) {
        if (localState.value.playingVoice == voice) {
            stopVoicePreview()
            return
        }
        adhanPlayer.stop()
        val started = adhanPlayer.play(
            voice = voice,
            customFile = customAdhanStore.customFile(),
            useAlarmStream = false,
            onComplete = { clearPlayingVoice(voice) },
        )
        localState.value = localState.value.copy(playingVoice = if (started) voice else null)
    }

    /** Stops any running adhan preview. */
    fun stopVoicePreview() {
        adhanPlayer.stop()
        localState.value = localState.value.copy(playingVoice = null)
    }

    /**
     * Imports a user-picked adhan document into private storage, persists its metadata and switches
     * the active voice to [AdhanVoice.CUSTOM]. Re-registers the prayer alarms afterwards.
     *
     * @param displayName resolved by the caller from [android.provider.OpenableColumns.DISPLAY_NAME].
     * @param mimeType resolved by the caller from [android.content.ContentResolver.getType].
     */
    fun importCustomAdhan(uri: Uri, displayName: String, mimeType: String?) {
        viewModelScope.launch {
            val meta = customAdhanStore.import(uri, displayName, mimeType)
            if (meta == null) {
                localState.value = localState.value.copy(
                    message = "تعذر استيراد الملف الصوتي · تأكد من أن الملف صالح وأن حجمه أقل من 40 ميغابايت",
                    messageIsError = true,
                )
                return@launch
            }
            settingsStore.update { it.copy(customAudioMeta = meta, adhanVoice = AdhanVoice.CUSTOM) }
            AdhanAlarmScheduler.rescheduleAll(container.context)
            localState.value = localState.value.copy(
                message = "تم استيراد ملف الأذان المخصص بنجاح",
                messageIsError = false,
            )
        }
    }

    /** Deletes the imported adhan file and falls back to the default voice when it was selected. */
    fun removeCustomAdhan() {
        viewModelScope.launch {
            adhanPlayer.stop()
            customAdhanStore.clear()
            settingsStore.update {
                it.copy(
                    customAudioMeta = null,
                    adhanVoice = if (it.adhanVoice == AdhanVoice.CUSTOM) AdhanVoice.ABDULBASIT else it.adhanVoice,
                )
            }
            AdhanAlarmScheduler.rescheduleAll(container.context)
            localState.value = localState.value.copy(
                playingVoice = null,
                message = "تم حذف الملف المخصص",
                messageIsError = false,
            )
        }
    }

    fun setDailyReminderEnabled(enabled: Boolean) = persist { it.copy(dailyReminderEnabled = enabled) }

    /** Persists the morning athkar reminder time as a zero-padded "HH:mm" string. */
    fun setMorningReminderTime(time: String) = persist { it.copy(morningReminderTime = time) }

    /** Persists the evening athkar reminder time as a zero-padded "HH:mm" string. */
    fun setEveningReminderTime(time: String) = persist { it.copy(eveningReminderTime = time) }

    // ------------------------------------------------------------------ duas

    fun setShareFormatWithAppName(enabled: Boolean) = persist { it.copy(shareFormatWithAppName = enabled) }

    // ------------------------------------------------------------------ backup & restore

    /** Writes a JSON backup to the document chosen through the Storage Access Framework. */
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val json = backupManager.exportToString()
            val ok = backupManager.writeToUri(uri, json)
            localState.value = localState.value.copy(
                message = if (ok) {
                    "تم إنشاء النسخة الاحتياطية بنجاح"
                } else {
                    "تعذر كتابة النسخة الاحتياطية في الموقع المحدد"
                },
                messageIsError = !ok,
            )
        }
    }

    /** Restores state from a previously exported JSON backup and re-registers the prayer alarms. */
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val raw = backupManager.readFromUri(uri)
            if (raw == null) {
                localState.value = localState.value.copy(
                    message = "تعذر قراءة الملف المحدد",
                    messageIsError = true,
                )
                return@launch
            }
            val result = backupManager.restoreFromString(raw)
            AdhanAlarmScheduler.rescheduleAll(container.context)
            localState.value = localState.value.copy(
                message = result.message,
                messageIsError = !result.success,
            )
        }
    }

    // ------------------------------------------------------------------ destructive actions

    /** Opens the "erase everything" confirmation dialog. */
    fun requestResetAll() {
        localState.value = localState.value.copy(showResetDialog = true)
    }

    fun dismissResetDialog() {
        localState.value = localState.value.copy(showResetDialog = false)
    }

    /**
     * Restores the app to factory state: default settings, empty rosary stats and cleared
     * favourites / counters / custom phrases / custom adhan file, then re-registers the alarms.
     */
    fun confirmResetAll() {
        viewModelScope.launch {
            adhanPlayer.stop()
            customAdhanStore.clear()
            settingsStore.set(AppSettings())
            userDataStore.setStats(TasbeehStats())
            userDataStore.setFavorites(emptySet())
            userDataStore.setCounters(emptyMap())
            userDataStore.setCustomPhrases(emptyList())
            AdhanAlarmScheduler.rescheduleAll(container.context)
            localState.value = localState.value.copy(
                showResetDialog = false,
                playingVoice = null,
                message = "تم مسح كل البيانات وإعادة التطبيق لحالته الافتراضية",
                messageIsError = false,
            )
        }
    }

    /** Zeroes the lifetime / cycles counters while keeping the user's settings. */
    fun resetLifetimeStats() {
        viewModelScope.launch {
            userDataStore.setStats(TasbeehStats(dailyDate = todayIso()))
            localState.value = localState.value.copy(
                message = "تمت إعادة تعيين إحصائيات المسبحة",
                messageIsError = false,
            )
        }
    }

    // ------------------------------------------------------------------ transient UI

    /** Dismisses the info / result dialog. */
    fun dismissMessage() {
        localState.value = localState.value.copy(message = null)
    }

    // ------------------------------------------------------------------ internals

    /** Atomically applies [transform] to the persisted settings, optionally re-registering alarms. */
    private fun persist(reschedule: Boolean = false, transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsStore.update(transform)
            if (reschedule) AdhanAlarmScheduler.rescheduleAll(container.context)
        }
    }

    private fun clearPlayingVoice(voice: AdhanVoice) {
        if (localState.value.playingVoice == voice) {
            localState.value = localState.value.copy(playingVoice = null)
        }
    }

    private fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    override fun onCleared() {
        super.onCleared()
        adhanPlayer.stop()
    }

    companion object {
        /** Lower / upper bound of the pre-adhan reminder window, in minutes. */
        const val MIN_ALERT_MINUTES = 0
        const val MAX_ALERT_MINUTES = 30

        val Factory = containerViewModelFactory { container -> SettingsViewModel(container) }
    }
}
