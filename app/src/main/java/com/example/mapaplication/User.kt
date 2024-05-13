package com.example.mapaplication

import com.yandex.mapkit.geometry.Point

class User() {
    lateinit var id: String
    lateinit var username: String

    constructor(_id: String, _username: String) : this() {
        id = _id
        username = _username
    }
}