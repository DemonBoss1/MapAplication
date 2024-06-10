package com.mammoth_empire.the_secret_of_cities

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