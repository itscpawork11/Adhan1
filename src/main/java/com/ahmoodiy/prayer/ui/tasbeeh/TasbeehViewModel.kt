package com.ahmoodiy.prayer.ui.tasbeeh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmoodiy.prayer.audio.BeadSoundPlayer
import com.ahmoodiy.prayer.data.TasbeehPhrasesData
import com.ahmoodiy.prayer.data.prefs.SettingsStore
import com.ahmoodiy.prayer.data.prefs.UserDataStore
import com.ahmoodiy.prayer.di.containerViewModelFactory
import com.ahmoodiy.prayer.domain.model.AppSettings
import com.ahmoodiy.prayer.domain.model.TasbeehPhrase
import com.ahmoodiy.prayer.domain.model.TasbeehStats
import com.ahmoodiy.prayer.util.Haptics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Everything the rosary screen renders. */
data class TasbeehUiState(
    val phrase: TasbeehPhrase = TasbeehPhrasesData.defaultPhrases.first(),
    val phrases: List<TasbeehPhrase> = TasbeehPhrasesData.defaultPhrases,
    val currentCount: Int = 0,
    val target: Int = 33,
    val stats: TasbeehStats = TasbeehStats(),
    val settings: AppSettings = AppSettings(),
    val showResetDialog: Boolean = false,
    val showPhraseManager: Boolean = false,
    val showAddPhraseDialog: Boolean = false,
    val customDraft: String = "",
    /** Increments every time a full cycle completes, so the UI can celebrate. */
    val celebrationToken: Int = 0,
) {
    val progress: Float get() = if (target <= 0) 0f else (currentCount.toFloat() / target).coerceIn(0f, 1f)
    val isComplete: Boolean get() = target > 0 && currentCount >= target
    val remaining: Int get() = (target - currentCount).coerceAtLeast(0)
}

/**
 * Drives the digital rosary.
 *
 * Counters are kept in an authoritative in-memory state and written through to [UserDataStore],
 * so rapid tapping can never lose a bead to an asynchronous read-back.
 */
class TasbeehViewModel(
    private val settingsStore: SettingsStore,
    private val userDataStore: UserDataStore,
    private val beadSoundPlayer: BeadSoundPlayer,
    private val haptics: Haptics,
) : ViewModel() {

    private val localState = MutableStateFlow(TasbeehUiState())

    private val today: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /** Derived UI state: authoritative counters + observed settings and custom phrases. */
    val state: StateFlow<TasbeehUiState> = combine(
        localState,
        settingsStore.settings,
        userDataStore.customPhrases,
    ) { local, settings, custom ->
        val phrases = TasbeehPhrasesData.defaultPhrases + custom
        val selected = phrases.firstOrNull { it.id == local.phrase.id } ?: phrases.first()
        local.copy(phrase = selected, phrases = phrases, settings = settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TasbeehUiState(),
    )

    init {
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            val stored = userDataStore.stats.first()
            val stats = rollDaily(stored)
            if (stats != stored) userDataStore.setStats(stats)
            localState.value = localState.value.copy(
                target = settings.tasbeehDefaultTarget,
                currentCount = stats.currentCount.coerceAtLeast(0),
                stats = stats,
            )
        }
    }

    fun tap() {
        val local = localState.value
        val stats = rollDaily(local.stats)

        val nextCount = local.currentCount + 1
        val completedNow = local.target > 0 && nextCount >= local.target

        val updatedStats = stats.copy(
            currentCount = nextCount,
            dailyTotal = stats.dailyTotal + 1,
            lifetimeTotal = stats.lifetimeTotal + 1,
            cyclesCompleted = stats.cyclesCompleted + if (completedNow) 1 else 0,
        )

        localState.value = local.copy(
            currentCount = nextCount,
            stats = updatedStats,
            celebrationToken = local.celebrationToken + if (completedNow) 1 else 0,
        )
        viewModelScope.launch { userDataStore.setStats(updatedStats) }

        val settings = state.value.settings
        if (settings.tasbeehSoundEnabled) {
            beadSoundPlayer.play(settings.tasbeehSoundType)
        }
        when {
            completedNow && settings.tasbeehLongHapticOnCycle -> haptics.doubleTap()
            settings.tasbeehHapticOnTap -> haptics.tap()
        }
    }

    fun requestReset() {
        val local = localState.value
        if (local.currentCount <= 0) return
        if (local.settings.tasbeehResetProtection) {
            localState.value = local.copy(showResetDialog = true)
        } else {
            resetCurrentRound()
        }
    }

    fun dismissResetDialog() {
        localState.value = localState.value.copy(showResetDialog = false)
    }

    fun resetCurrentRound() {
        val local = localState.value
        val updated = rollDaily(local.stats).copy(currentCount = 0)
        localState.value = local.copy(currentCount = 0, stats = updated, showResetDialog = false)
        viewModelScope.launch { userDataStore.setStats(updated) }
        haptics.cycleComplete()
    }

    fun selectPhrase(phrase: TasbeehPhrase) {
        val local = localState.value
        val updated = rollDaily(local.stats).copy(currentCount = 0)
        localState.value = local.copy(
            phrase = phrase,
            target = if (phrase.defaultTarget > 0) phrase.defaultTarget else local.target,
            currentCount = 0,
            stats = updated,
            showPhraseManager = false,
        )
        viewModelScope.launch { userDataStore.setStats(updated) }
    }

    fun setTarget(target: Int) {
        localState.value = localState.value.copy(target = target.coerceIn(1, 100_000))
    }

    /** Quick toggles surfaced in the screen header. */
    fun toggleSound() = viewModelScope.launch {
        settingsStore.update { it.copy(tasbeehSoundEnabled = !it.tasbeehSoundEnabled) }
    }

    fun toggleHaptics() = viewModelScope.launch {
        settingsStore.update { it.copy(tasbeehHapticOnTap = !it.tasbeehHapticOnTap) }
    }

    fun cycleSoundType() = viewModelScope.launch {
        settingsStore.update { settings ->
            val order = com.ahmoodiy.prayer.domain.model.TasbeehSoundType.ordered
            val next = order[(order.indexOf(settings.tasbeehSoundType) + 1) % order.size]
            settings.copy(tasbeehSoundType = next, tasbeehSoundEnabled = true)
        }
    }

    fun togglePhraseManager() {
        localState.value = localState.value.copy(showPhraseManager = !localState.value.showPhraseManager)
    }

    fun openAddPhraseDialog() {
        localState.value = localState.value.copy(showAddPhraseDialog = true, customDraft = "")
    }

    fun dismissAddPhraseDialog() {
        localState.value = localState.value.copy(showAddPhraseDialog = false, customDraft = "")
    }

    fun updateCustomDraft(value: String) {
        localState.value = localState.value.copy(customDraft = value)
    }

    fun saveCustomPhrase() {
        val text = localState.value.customDraft.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            val existing = userDataStore.customPhrases.first()
            val phrase = TasbeehPhrase(
                id = "custom-${System.currentTimeMillis()}",
                arabicText = text,
                virtue = "ذكر مخصص أضافه المستخدم",
                defaultTarget = localState.value.settings.tasbeehDefaultTarget,
                isCustom = true,
            )
            userDataStore.setCustomPhrases(existing + phrase)
            selectPhrase(phrase)
            localState.value = localState.value.copy(showAddPhraseDialog = false, customDraft = "")
        }
    }

    fun deletePhrase(phrase: TasbeehPhrase) {
        if (!phrase.isCustom) return
        viewModelScope.launch {
            val existing = userDataStore.customPhrases.first()
            userDataStore.setCustomPhrases(existing.filterNot { it.id == phrase.id })
            if (localState.value.phrase.id == phrase.id) {
                val fallback = TasbeehPhrasesData.defaultPhrases.first()
                localState.value = localState.value.copy(phrase = fallback, currentCount = 0)
            }
        }
    }

    /** Clears the lifetime / cycles counters after an explicit confirmation. */
    fun resetLifetimeStats() {
        val fresh = TasbeehStats(dailyDate = today)
        localState.value = localState.value.copy(currentCount = 0, stats = fresh)
        viewModelScope.launch { userDataStore.setStats(fresh) }
    }

    private fun rollDaily(stats: TasbeehStats): TasbeehStats =
        if (stats.dailyDate == today) stats else stats.copy(dailyTotal = 0, dailyDate = today)

    override fun onCleared() {
        super.onCleared()
        haptics.release()
    }

    companion object {
        val Factory = containerViewModelFactory { container ->
            TasbeehViewModel(
                settingsStore = container.settingsStore,
                userDataStore = container.userDataStore,
                beadSoundPlayer = container.beadSoundPlayer,
                haptics = container.haptics,
            )
        }
    }
}
