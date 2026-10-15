package com.ahmoodiy.prayer.domain.model

/** A tasbeeh formula with its default target and reported virtue. */
data class TasbeehPhrase(
    val id: String,
    val arabicText: String,
    val virtue: String,
    val defaultTarget: Int,
    val isCustom: Boolean = false,
)
