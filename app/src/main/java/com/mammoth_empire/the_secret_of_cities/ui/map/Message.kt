package com.mammoth_empire.the_secret_of_cities.ui.map

import java.util.ArrayList

class Message() {

    lateinit var interestPointId: String
    lateinit var message: String
    lateinit var userId: String
    lateinit var date: String
    lateinit var reviewList: ArrayList<Boolean>
    constructor(_interestPointId: String, _message: String, _userId: String, _date: String, _reviewList: ArrayList<Boolean>) : this() {
        interestPointId = _interestPointId
        message = _message
        userId = _userId
        date = _date
        reviewList = _reviewList
    }
}