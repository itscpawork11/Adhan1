package com.ahmoodiy.prayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmoodiy.prayer.data.prefs.SettingsStore
import com.ahmoodiy.prayer.di.containerViewModelFactory
import com.ahmoodiy.prayer.domain.model.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Exposes the user settings to the whole app tree.
 * Screens that mutate settings use [com.ahmoodiy.prayer.ui.settings.SettingsViewModel] instead.
 */
class AppViewModel(settingsStore: SettingsStore) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings(),
    )

    companion object {
        val Factory = containerViewModelFactory { container -> AppViewModel(container.settingsStore) }
    }
}
