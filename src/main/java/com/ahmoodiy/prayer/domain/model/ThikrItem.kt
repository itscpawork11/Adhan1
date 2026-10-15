package com.ahmoodiy.prayer.domain.model

/** A single thikr / dua entry from the offline Hisn al-Muslim dataset. */
data class ThikrItem(
    val id: Int,
    val category: ThikrCategory,
    val categoryArabic: String,
    val title: String? = null,
    val text: String,
    /** The legislated repetition count (1, 3, 33, 100 ...). */
    val count: Int,
    val virtue: String,
    val source: String,
    val isQuranVerse: Boolean = false,
)
