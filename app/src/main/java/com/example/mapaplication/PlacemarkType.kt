package com.example.mapaplication

enum class PlacemarkType {
    ARCHITECTURE,
    CAFE,
    HOTEL
}

data class PlacemarkUserData(
    val name: String,
    val type: PlacemarkType,
)