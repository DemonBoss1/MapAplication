package com.example.mapaplication

import java.util.Date

class Message() {

    //lateinit var interestPointID: String
    lateinit var message: String
    lateinit var userId: String
    lateinit var date: String
    constructor(_message: String, _userId: String, _date: String) : this() {
        message = _message
        userId = _userId
        date = _date
    }
}