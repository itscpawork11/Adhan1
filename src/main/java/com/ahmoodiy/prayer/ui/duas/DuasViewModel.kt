package com.ahmoodiy.prayer.ui.duas

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the Duas (الأدعية) screen renders. */
data class DuasUiState(
    /** Duas matching the active category, search query and favourites filter. */
    val items: List<ThikrItem> = emptyList(),
    /** Active category, or `null` for "الكل" (all dua categories). */
    val selectedCategory: ThikrCategory? = null,
    val query: String = "",
    val favoritesOnly: Boolean = false,
    /** Per-dua tap counts keyed by [ThikrItem.id]. */
    val counters: Map<Int, Int> = emptyMap(),
    /** Ids of the duas the user marked as favourite. */
    val favorites: Set<Int> = emptySet(),
    val settings: AppSettings = AppSettings(),
) {
    /** Number of duas currently visible. */
    val resultCount: Int get() = items.size
}

/**
 * Mutable, authoritative slice of the screen state.
 *
 * Counters and favourites live here so a fast tap can never be lost to an asynchronous
 * read-back from DataStore; the persisted maps are only ever written, never read back to
 * drive the UI once the store has been seeded.
 */
private data class LocalState(
    val selectedCategory: ThikrCategory? = null,
    val query: String = "",
    val favoritesOnly: Boolean = false,
    val counters: Map<Int, Int> = emptyMap(),
    val favorites: Set<Int> = emptySet(),
    /** Shuffled dua ids; empty means "keep the dataset order". */
    val order: List<Int> = emptyList(),
    /** True once the persisted maps have been read into this state. */
    val seeded: Boolean = false,
)

/**
 * Drives the Duas screen: category/search filtering, favourites filter, shuffling and the
 * per-dua counters.
 *
 * The counters and favourites maps are kept in [localState] and written through to
 * [UserDataStore] as a whole map, so rapid taps are never dropped.
 */
class DuasViewModel(
    private val settingsStore: SettingsStore,
    private val userDataStore: UserDataStore,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalState())

    /** Derived UI state: authoritative counters/favourites + observed settings. */
    val state: StateFlow<DuasUiState> = combine(
        localState,
        settingsStore.settings,
        userDataStore.counters,
        userDataStore.favorites,
    ) { local, settings, storedCounters, storedFavorites ->
        val counters = if (local.seeded) local.counters else storedCounters
        val favorites = if (local.seeded) local.favorites else storedFavorites
        val effective = local.copy(counters = counters, favorites = favorites)
        DuasUiState(
            items = orderItems(matchingItems(effective), local.order),
            selectedCategory = local.selectedCategory,
            query = local.query,
            favoritesOnly = local.favoritesOnly,
            counters = counters,
            favorites = favorites,
            settings = settings,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DuasUiState(items = duasPool(null)),
    )

    init {
        viewModelScope.launch {
            val counters = userDataStore.counters.first()
            val favorites = userDataStore.favorites.first()
            localState.update { current ->
                if (current.seeded) {
                    current
                } else {
                    current.copy(counters = counters, favorites = favorites, seeded = true)
                }
            }
        }
    }

    /** Selects one of the dua categories, or `null` to show them all ("الكل"). */
    fun selectCategory(category: ThikrCategory?) {
        localState.update { it.copy(selectedCategory = category, order = emptyList()) }
    }

    /** Updates the free-text search query (tashkeel-insensitive). */
    fun updateQuery(value: String) {
        localState.update { it.copy(query = value, order = emptyList()) }
    }

    /** Toggles the "favourites only" restriction. */
    fun toggleFavoritesOnly() {
        localState.update { it.copy(favoritesOnly = !it.favoritesOnly, order = emptyList()) }
    }

    /** Randomises the order of the currently matching duas. */
    fun shuffle() {
        val local = localState.value
        val order = matchingItems(local).map { it.id }.shuffled()
        localState.value = local.copy(order = order)
    }

    /** Advances the counter for [item]; wraps back to zero once the target is reached. */
    fun incrementCounter(item: ThikrItem) {
        val local = localState.value
        val current = local.counters[item.id] ?: 0
        val updated = local.counters + (item.id to nextCount(current, item.count))
        localState.value = local.copy(counters = updated, seeded = true)
        viewModelScope.launch { userDataStore.setCounters(updated) }
    }

    /** Resets the counter of [item] back to zero. */
    fun resetCounter(item: ThikrItem) {
        val local = localState.value
        val updated = local.counters + (item.id to 0)
        localState.value = local.copy(counters = updated, seeded = true)
        viewModelScope.launch { userDataStore.setCounters(updated) }
    }

    /** Adds or removes [item] from the favourites set. */
    fun toggleFavorite(item: ThikrItem) {
        val local = localState.value
        val favorites = local.favorites.toMutableSet().apply {
            if (!add(item.id)) remove(item.id)
        }
        localState.value = local.copy(favorites = favorites, seeded = true)
        viewModelScope.launch { userDataStore.setFavorites(favorites) }
    }

    private fun nextCount(current: Int, target: Int): Int = when {
        target <= 0 -> current + 1
        current >= target -> 0
        else -> current + 1
    }

    companion object {
        val Factory = containerViewModelFactory { container ->
            DuasViewModel(container.settingsStore, container.userDataStore)
        }
    }
}

/** Duas belonging to [category], or every dua category when [category] is `null`. */
private fun duasPool(category: ThikrCategory?): List<ThikrItem> =
    if (category == null) {
        HisnAlMuslimData.items.filter { it.category in ThikrCategory.duasCategories }
    } else {
        HisnAlMuslimData.byCategory(category)
    }

/** Applies the category, search and favourites filters to the dua pool. */
private fun matchingItems(local: LocalState): List<ThikrItem> {
    val query = Tashkeel.strip(local.query).trim()
    return duasPool(local.selectedCategory).filter { item ->
        (!local.favoritesOnly || item.id in local.favorites) &&
            (query.isEmpty() || matches(item, query))
    }
}

/** Tashkeel-insensitive match across the dua text, title, source and virtue. */
private fun matches(item: ThikrItem, query: String): Boolean = listOf(
    item.text,
    item.title.orEmpty(),
    item.source,
    item.virtue,
).any { Tashkeel.strip(it).contains(query, ignoreCase = true) }

/** Re-orders [items] according to a shuffled id list; unknown ids keep the dataset order. */
private fun orderItems(items: List<ThikrItem>, order: List<Int>): List<ThikrItem> {
    if (order.isEmpty()) return items
    val rank = order.withIndex().associate { (index, id) -> id to index }
    return items.sortedBy { rank[it.id] ?: Int.MAX_VALUE }
}
