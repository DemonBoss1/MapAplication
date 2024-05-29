package com.example.mapaplication.ui.map

enum class PlacemarkType {
    ARCHITECTURE,
    CAFE,
    HOTEL
}

class PlacemarkUserData() {
    lateinit var id: String
    lateinit var title: String
    lateinit var description: String
    lateinit var type: PlacemarkType

    constructor(_id: String, _title: String, _description: String, _type: PlacemarkType) : this() {
        id = _id
        title = _title
        description = _description
        type = _type
    }
}