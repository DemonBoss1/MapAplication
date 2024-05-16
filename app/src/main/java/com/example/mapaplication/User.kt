package com.example.mapaplication

import android.net.Uri
import com.yandex.mapkit.geometry.Point

class User() {
    lateinit var id: String
    lateinit var username: String
    lateinit var imageUri: String

    constructor(_id: String, _username: String, _imageUri: String) : this() {
        id = _id
        username = _username
        imageUri = _imageUri
    }
}