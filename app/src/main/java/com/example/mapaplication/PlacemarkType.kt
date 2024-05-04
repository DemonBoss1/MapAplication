package com.example.mapaplication

import com.yandex.mapkit.geometry.Point

enum class PlacemarkType {
    ARCHITECTURE,
    CAFE,
    HOTEL
}

class PlacemarkUserData() {
    lateinit var title: String
    lateinit var description: String
    lateinit var type: PlacemarkType

    constructor(_title: String, _description: String, _type: PlacemarkType) : this() {
        title = _title
        description = _description
        type = _type
    }
}