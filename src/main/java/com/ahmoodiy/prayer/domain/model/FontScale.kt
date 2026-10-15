package com.ahmoodiy.prayer.domain.model

/** Reader font size scale offered in Settings. */
enum class FontScale(val id: String, val label: String, val multiplier: Float) {
    SMALL("sm", "A-", 0.92f),
    BASE("base", "A", 1.0f),
    LARGE("lg", "A+", 1.12f),
    XLARGE("xl", "A++", 1.24f);

    companion object {
        val ordered: List<FontScale> = listOf(SMALL, BASE, LARGE, XLARGE)

        fun fromId(id: String?): FontScale = entries.firstOrNull { it.id.equals(id, true) } ?: BASE
    }
}
