package com.example.mapaplication

import java.util.Date

class Message() {

    lateinit var message: String
    lateinit var userId: String
    lateinit var date: Date
    constructor(_message: String, _userId: String, _date: Date) : this() {
        message = _message
        userId = _userId
        date = _date
    }
}