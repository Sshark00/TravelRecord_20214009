package com.example.travelrecord_20214009

data class TravelRecord(
    val id: Long = 0,
    val title: String,
    val date: String,
    val memo: String = "",
    val photoPath: String = "",
    val isFavorite: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
