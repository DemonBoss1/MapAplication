package com.example.mapaplication

enum class PlacemarkType {
    ARCHITECTURE,
    CAFE,
    HOTEL
}

data class PlacemarkUserData(
    val title: String,
    val description: String,
    val type: PlacemarkType,
)