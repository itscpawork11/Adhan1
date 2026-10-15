package com.ahmoodiy.prayer.domain.model

/** One of the 27 Egyptian governorates used for prayer-time calculation. */
data class Governorate(
    val id: String,
    val arabicName: String,
    val englishName: String,
    val lat: Double,
    val lng: Double,
)
