package com.ahmoodiy.prayer.ui.athkar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmoodiy.prayer.data.HisnAlMuslimData
import com.ahmoodiy.prayer.data.prefs.SettingsStore
import com.ahmoodiy.prayer.data.prefs.UserDataStore
import com.ahmoodiy.prayer.di.containerViewModelFactory
import com.ahmoodiy.prayer.domain.model.AppSettings
import com.ahmoodiy.prayer.domain.model.ThikrCategory
import com.ahmoodiy.prayer.domain.model.ThikrItem
import com.ahmoodiy.prayer.util.Tashkeel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Everything the athkar (الأذكار) screen renders.
 *
 * The filter/selection fields live in the "local" layer, while [settings], [counters] and
 * [favorites] are merged in from the stores (counters and favourites are authoritative in memory
 * and written through to disk, never read back mid-interaction).
 */
data class AthkarUiState(
    val query: String = "",
    val category: ThikrCategory = ThikrCategory.MORNING,
    val favoritesOnly: Boolean = false,
    /** Explicit id order applied after shuffle; empty means "dataset order". */
    val shuffleOrder: List<Int> = emptyList(),
    val settings: AppSettings = AppSettings(),
    /** Per-thikr progress keyed by [ThikrItem.id]. */
    val counters: Map<Int, Int> = emptyMap(),
    val favorites: Set<Int> = emptySet(),
) {
    /** Total number of entries in the offline dataset. */
    val totalCount: Int get() = HisnAlMuslimData.totalCount

    /** True when the free-text search governs the list instead of the selected category. */
    val searchActive: Boolean get() = query.isNotBlank()

    /** Current repetition count for a thikr, defaulting to zero. */
    fun counterOf(item: ThikrItem): Int = counters[item.id] ?: 0

    /** True once the thikr has reached its legislated repetition count. */
    fun isComplete(item: ThikrItem): Boolean = item.count > 0 && counterOf(item) >= item.count

    fun isFavorite(item: ThikrItem): Boolean = item.id in favorites

    /**
     * The filtered, ordered list the screen renders.
     *
     * Search — when active — spans every category; otherwise the selected [category] applies.
     * Favourites-only, the auto-hide of completed thikr and the shuffle order are applied on top.
     */
    val displayedItems: List<ThikrItem>
        get() {
            val base = if (searchActive) {
                val needle = Tashkeel.strip(query)
                HisnAlMuslimData.items.filter { item ->
                    Tashkeel.strip(item.text).contains(needle) ||
                        Tashkeel.strip(item.title).contains(needle) ||
                        Tashkeel.strip(item.categoryArabic).contains(needle) ||
                        Tashkeel.strip(item.virtue).contains(needle)
                }
            } else {
                HisnAlMuslimData.byCategory(category)
            }

            var list = base
            if (favoritesOnly) list = list.filter { it.id in favorites }
            if (settings.autoHideCompletedThikr) list = list.filterNot { isComplete(it) }
            if (shuffleOrder.isNotEmpty()) {
                val order = shuffleOrder.withIndex().associate { (index, id) -> id to index }
                list = list.sortedBy { order[it.id] ?: Int.MAX_VALUE }
            }
            return list
        }
}

/**
 * Drives the offline Hisn al-Muslim library.
 *
 * Per-thikr counters and favourites are kept in authoritative in-memory maps and written through
 * to [UserDataStore], so rapid tapping can never lose an update to an asynchronous read-back.
 */
class AthkarViewModel(
    private val settingsStore: SettingsStore,
    private val userDataStore: UserDataStore,
) : ViewModel() {

    private val localState = MutableStateFlow(AthkarUiState())

    /** Authoritative counter map; null until the user changes something (persisted value wins). */
    private val countersOverride = MutableStateFlow<Map<Int, Int>?>(null)

    /** Authoritative favourites set; null until the user changes something. */
    private val favoritesOverride = MutableStateFlow<Set<Int>?>(null)

    /** Derived UI state: local selection/filters + observed settings, counters and favourites. */
    val state: StateFlow<AthkarUiState> = combine(
        localState,
        settingsStore.settings,
        userDataStore.counters,
        userDataStore.favorites,
    ) { local, settings, persistedCounters, persistedFavorites ->
        local.copy(
            settings = settings,
            counters = countersOverride.value ?: persistedCounters,
            favorites = favoritesOverride.value ?: persistedFavorites,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AthkarUiState(),
    )

    init {
        // Seed the authoritative maps once so the first mutation never clobbers persisted data.
        viewModelScope.launch {
            if (countersOverride.value == null) countersOverride.value = userDataStore.counters.first()
            if (favoritesOverride.value == null) favoritesOverride.value = userDataStore.favorites.first()
        }
    }

    /** Authoritative counter map, falling back to the last observed persisted value. */
    private fun authoritativeCounters(): Map<Int, Int> = countersOverride.value ?: state.value.counters

    /** Authoritative favourites set, falling back to the last observed persisted value. */
    private fun authoritativeFavorites(): Set<Int> = favoritesOverride.value ?: state.value.favorites

    /** Updates the search query and clears any stale shuffle ordering. */
    fun setQuery(value: String) {
        localState.value = localState.value.copy(query = value, shuffleOrder = emptyList())
    }

    /** Selects a category and clears any stale shuffle ordering. */
    fun setCategory(category: ThikrCategory) {
        localState.value = localState.value.copy(category = category, shuffleOrder = emptyList())
    }

    /** Toggles the "favourites only" filter. */
    fun toggleFavoritesOnly() {
        localState.value = localState.value.copy(
            favoritesOnly = !localState.value.favoritesOnly,
            shuffleOrder = emptyList(),
        )
    }

    /** Adds or removes a thikr from the persisted favourites. */
    fun toggleFavorite(id: Int) {
        val current = authoritativeFavorites()
        val updated = if (id in current) current - id else current + id
        favoritesOverride.value = updated
        viewModelScope.launch { userDataStore.setFavorites(updated) }
    }

    /**
     * Advances a thikr's repetition counter.
     *
     * Reaching the legislated count marks the thikr complete; pressing again wraps the counter
     * back to zero so the user can start a fresh cycle without a separate reset.
     */
    fun increment(item: ThikrItem) {
        val current = authoritativeCounters()
        val value = current[item.id] ?: 0
        val target = item.count.coerceAtLeast(1)
        val next = if (value >= target) 0 else value + 1
        val updated = if (next == 0) current - item.id else current + (item.id to next)
        countersOverride.value = updated
        viewModelScope.launch { userDataStore.setCounters(updated) }
    }

    /** Clears a single thikr's counter. */
    fun resetCounter(item: ThikrItem) {
        val current = authoritativeCounters()
        if (current[item.id] == null) return
        val updated = current - item.id
        countersOverride.value = updated
        viewModelScope.launch { userDataStore.setCounters(updated) }
    }

    /** Randomises the order of the currently displayed items. */
    fun shuffle() {
        val ids = state.value.displayedItems.map { it.id }.shuffled()
        localState.value = localState.value.copy(shuffleOrder = ids)
    }

    companion object {
        val Factory = containerViewModelFactory { container ->
            AthkarViewModel(container.settingsStore, container.userDataStore)
        }
    }
}
