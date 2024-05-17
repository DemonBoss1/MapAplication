package com.example.mapaplication.ui.map

import java.util.Date

class Message() {

    lateinit var interestPointId: String
    lateinit var message: String
    lateinit var userId: String
    lateinit var date: String
    constructor(_interestPointId: String, _message: String, _userId: String, _date: String) : this() {
        interestPointId = _interestPointId
        message = _message
        userId = _userId
        date = _date
    }
}