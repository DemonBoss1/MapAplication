package com.example.mapaplication

import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database

class DataBase {
    lateinit var database:FirebaseDatabase
    lateinit var pointReference: DatabaseReference

    companion object {
        private lateinit var instance: DataBase

        fun get(): DataBase {
            return instance;
        }
    }

    init {
        if (instance == null) {
            database = Firebase.database
            pointReference = database.getReference("Point")
            instance = this
        }
    }
}