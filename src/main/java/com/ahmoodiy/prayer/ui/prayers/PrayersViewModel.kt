package com.ahmoodiy.prayer.ui.prayers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmoodiy.prayer.alarm.AdhanAlarmScheduler
import com.ahmoodiy.prayer.data.GovernoratesData
import com.ahmoodiy.prayer.data.prefs.SettingsStore
import com.ahmoodiy.prayer.di.containerViewModelFactory
import com.ahmoodiy.prayer.domain.model.AdhanVoice
import com.ahmoodiy.prayer.domain.model.AppSettings
import com.ahmoodiy.prayer.domain.model.Governorate
import com.ahmoodiy.prayer.domain.model.PrayerKey
import com.ahmoodiy.prayer.domain.prayer.EgyptPrayerEngine
import com.ahmoodiy.prayer.domain.prayer.EgyptPrayerEngine.PrayerSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Everything the prayer-times screen renders.
 *
 * The schedule is recomputed for the selected governorate on every emission, so the screen
 * always reflects today's calculated times. [nowMillis] is refreshed once per second by the
 * screen's ticker (see [PrayersViewModel.tick]).
 */
data class PrayersUiState(
    val settings: AppSettings = AppSettings(),
    val governorate: Governorate = GovernoratesData.default,
    val schedule: PrayerSchedule = EgyptPrayerEngine.calculateToday(
        GovernoratesData.default.lat,
        GovernoratesData.default.lng,
    ),
    /** Wall-clock instant the countdowns are measured against. */
    val nowMillis: Long = System.currentTimeMillis(),
    /** Whether the "طريقة الحساب / التوقيت الصيفي" dialog is visible. */
    val showMethodInfo: Boolean = false,
) {
    /** All six moments in display order (الفجر … العشاء, including الشروق). */
    val ordered: List<PrayerKey> get() = PrayerKey.ordered

    /** The window that currently contains [nowMillis]; falls back to العشاء before dawn. */
    val currentKey: PrayerKey
        get() = ordered.lastOrNull { nowMillis >= schedule.millisFor(it) } ?: PrayerKey.ISHA

    /** The next upcoming moment; wraps to tomorrow's الفجر after العشاء. */
    val nextKey: PrayerKey
        get() = ordered.firstOrNull { schedule.millisFor(it) > nowMillis } ?: PrayerKey.FAJR

    /** Epoch-millis of the next moment, rolling over to tomorrow's الفجر when needed. */
    val nextMillis: Long
        get() = ordered.firstOrNull { schedule.millisFor(it) > nowMillis }
            ?.let { schedule.millisFor(it) }
            ?: (schedule.fajr + DAY_MILLIS)

    /** Epoch-millis of [key] today (raw engine result, before per-prayer offsets). */
    fun millisFor(key: PrayerKey): Long = schedule.millisFor(key)

    /** True when [key] is the very next upcoming moment (today). */
    fun isNext(key: PrayerKey): Boolean = key == nextKey && schedule.millisFor(key) > nowMillis

    companion object {
        const val DAY_MILLIS: Long = 24L * 60L * 60L * 1000L
    }
}

/**
 * Drives the prayer-times (مواقيت الصلاة) screen.
 *
 * All editable values live in [SettingsStore]; any change that affects the calculated timetable
 * re-registers the adhan alarms through [AdhanAlarmScheduler.rescheduleAll].
 */
class PrayersViewModel(
    private val settingsStore: SettingsStore,
    private val context: Context,
) : ViewModel() {

    /** Mutable slice of the state (clock + dialog visibility) that is not persisted. */
    private data class LocalState(
        val nowMillis: Long = System.currentTimeMillis(),
        val showMethodInfo: Boolean = false,
    )

    private val localState = MutableStateFlow(LocalState())

    /** Derived UI state: observed settings + freshly computed schedule + the ticking clock. */
    val state: StateFlow<PrayersUiState> = combine(
        localState,
        settingsStore.settings,
    ) { local, settings ->
        val governorate = GovernoratesData.byId(settings.governorateId)
        val schedule = EgyptPrayerEngine.calculate(
            Calendar.getInstance(),
            governorate.lat,
            governorate.lng,
        )
        PrayersUiState(
            settings = settings,
            governorate = governorate,
            schedule = schedule,
            nowMillis = local.nowMillis,
            showMethodInfo = local.showMethodInfo,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PrayersUiState(),
    )

    /** Refreshes the clock so the live countdowns advance. Called once per second. */
    fun tick() {
        localState.update { it.copy(nowMillis = System.currentTimeMillis()) }
    }

    /** Opens the calculation-method / daylight-saving explanation dialog. */
    fun showMethodInfo() {
        localState.update { it.copy(showMethodInfo = true) }
    }

    /** Dismisses the calculation-method dialog. */
    fun dismissMethodInfo() {
        localState.update { it.copy(showMethodInfo = false) }
    }

    /** Persists the selected governorate and re-registers the adhan alarms. */
    fun selectGovernorate(id: String) = viewModelScope.launch {
        settingsStore.update { it.copy(governorateId = id) }
        AdhanAlarmScheduler.rescheduleAll(context)
    }

    /** Cycles the alert mode of [key] (أذان ← تنبيه ← صامت) and re-registers the alarms. */
    fun cycleMode(key: PrayerKey) = viewModelScope.launch {
        if (!key.isPrayer) return@launch
        settingsStore.update { current ->
            val next = current.modeFor(key).next()
            current.copy(prayerModes = current.prayerModes + (key to next))
        }
        AdhanAlarmScheduler.rescheduleAll(context)
    }

    /** Nudges the manual offset of [key] by [delta] minutes; the model clamps it to its range. */
    fun adjustOffset(key: PrayerKey, delta: Int) = viewModelScope.launch {
        settingsStore.update { current ->
            val updated = current.prayerOffsets.minutesFor(key) + delta
            current.copy(prayerOffsets = current.prayerOffsets.updated(key, updated))
        }
        AdhanAlarmScheduler.rescheduleAll(context)
    }

    /** Selects the adhan recitation voice (does not affect alarm timing). */
    fun selectVoice(voice: AdhanVoice) = viewModelScope.launch {
        settingsStore.update { it.copy(adhanVoice = voice) }
    }

    /** Sets the pre-adhan alert lead time in minutes (0..30) and re-registers the alarms. */
    fun setAlertTiming(minutes: Int) = viewModelScope.launch {
        settingsStore.update { it.copy(prayerAlertTiming = minutes.coerceIn(0, 30)) }
        AdhanAlarmScheduler.rescheduleAll(context)
    }

    /** Enables / disables the Fajr alert and re-registers the alarms. */
    fun toggleFajrAlert() = viewModelScope.launch {
        settingsStore.update { it.copy(fajrAlertEnabled = !it.fajrAlertEnabled) }
        AdhanAlarmScheduler.rescheduleAll(context)
    }

    companion object {
        val Factory = containerViewModelFactory { container ->
            PrayersViewModel(settingsStore = container.settingsStore, context = container.context)
        }
    }
}
