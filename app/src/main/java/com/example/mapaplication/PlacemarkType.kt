package com.example.mapaplication

enum class PlacemarkType {
    YELLOW,
    GREEN,
    RED
}

data class PlacemarkUserData(
    val name: String,
    val type: PlacemarkType,
)
