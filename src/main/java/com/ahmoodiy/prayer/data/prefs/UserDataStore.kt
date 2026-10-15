package com.ahmoodiy.prayer.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ahmoodiy.prayer.domain.model.TasbeehPhrase
import com.ahmoodiy.prayer.domain.model.TasbeehStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_data")

/**
 * Persists everything that is not a setting: rosary statistics, per-thikr counters,
 * favourites and user-created tasbeeh phrases.
 */
class UserDataStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val stats: Flow<TasbeehStats> = context.userDataStore.data.map { prefs ->
        TasbeehStats(
            currentCount = prefs[Keys.CURRENT_COUNT] ?: 0,
            dailyTotal = prefs[Keys.DAILY_TOTAL] ?: 0,
            lifetimeTotal = prefs[Keys.LIFETIME_TOTAL] ?: 0,
            cyclesCompleted = prefs[Keys.CYCLES] ?: 0,
            dailyDate = prefs[Keys.DAILY_DATE] ?: "",
        )
    }

    val favorites: Flow<Set<Int>> = context.userDataStore.data.map { prefs ->
        decodeIds(prefs[Keys.FAVORITES])
    }

    val counters: Flow<Map<Int, Int>> = context.userDataStore.data.map { prefs ->
        decodeCounters(prefs[Keys.COUNTERS])
    }

    val customPhrases: Flow<List<TasbeehPhrase>> = context.userDataStore.data.map { prefs ->
        decodePhrases(prefs[Keys.CUSTOM_PHRASES])
    }

    suspend fun setStats(stats: TasbeehStats) {
        context.userDataStore.edit { prefs ->
            prefs[Keys.CURRENT_COUNT] = stats.currentCount
            prefs[Keys.DAILY_TOTAL] = stats.dailyTotal
            prefs[Keys.LIFETIME_TOTAL] = stats.lifetimeTotal
            prefs[Keys.CYCLES] = stats.cyclesCompleted
            prefs[Keys.DAILY_DATE] = stats.dailyDate
        }
    }

    suspend fun setFavorites(ids: Set<Int>) {
        context.userDataStore.edit { prefs -> prefs[Keys.FAVORITES] = ids.sorted().joinToString(",") }
    }

    suspend fun setCounters(counters: Map<Int, Int>) {
        context.userDataStore.edit { prefs ->
            prefs[Keys.COUNTERS] = counters.entries
                .sortedBy { it.key }
                .joinToString(";") { "${it.key}:${it.value}" }
        }
    }

    suspend fun setCustomPhrases(phrases: List<TasbeehPhrase>) {
        context.userDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_PHRASES] = json.encodeToString(phrases.map { it.toRecord() })
        }
    }

    private fun decodeIds(raw: String?): Set<Int> =
        raw?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.toSet()
            .orEmpty()

    private fun decodeCounters(raw: String?): Map<Int, Int> = buildMap {
        raw?.split(';')?.forEach { entry ->
            val parts = entry.split(':')
            if (parts.size == 2) {
                val id = parts[0].trim().toIntOrNull()
                val value = parts[1].trim().toIntOrNull()
                if (id != null && value != null) put(id, value)
            }
        }
    }

    private fun decodePhrases(raw: String?): List<TasbeehPhrase> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<PhraseRecord>>(raw) }
            .getOrDefault(emptyList())
            .map { it.toModel() }
    }

    @Serializable
    private data class PhraseRecord(
        val id: String,
        val arabicText: String,
        val virtue: String,
        val defaultTarget: Int,
    )

    private fun TasbeehPhrase.toRecord() = PhraseRecord(id, arabicText, virtue, defaultTarget)
    private fun PhraseRecord.toModel() = TasbeehPhrase(id, arabicText, virtue, defaultTarget, isCustom = true)

    private object Keys {
        val CURRENT_COUNT = intPreferencesKey("tasbeeh_current_count")
        val DAILY_TOTAL = intPreferencesKey("tasbeeh_daily_total")
        val LIFETIME_TOTAL = intPreferencesKey("tasbeeh_lifetime_total")
        val CYCLES = intPreferencesKey("tasbeeh_cycles")
        val DAILY_DATE = stringPreferencesKey("tasbeeh_daily_date")
        val FAVORITES = stringPreferencesKey("favorite_athkar_ids")
        val COUNTERS = stringPreferencesKey("athkar_counters")
        val CUSTOM_PHRASES = stringPreferencesKey("custom_tasbeeh_phrases")
    }
}
