package com.example.mapaplication

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DataBase private constructor() {

    private val POINT_KEY = "InterestPoint"

    private var firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance("https://the-secret-of-cities-default-rtdb.europe-west1.firebasedatabase.app")
    private var pointReference: DatabaseReference = firebaseDatabase.getReference(POINT_KEY)

    init {
        dataFromDB
    }

    private val dataFromDB: Unit
        get() {
            val valueEventListener: ValueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {}
                override fun onCancelled(error: DatabaseError) {}
            }
            pointReference.addValueEventListener(valueEventListener)
        }

    companion object {
        private var dataBase: DataBase? = null
        fun updateFirebaseDatabase() {
            dataBase!!.firebaseDatabase =
                FirebaseDatabase.getInstance("https://the-secret-of-cities-default-rtdb.europe-west1.firebasedatabase.app")
            dataBase!!.pointReference = dataBase!!.firebaseDatabase.getReference(
                dataBase!!.POINT_KEY
            )
            dataBase!!.dataFromDB
        }

        fun getRef(): DatabaseReference {
            return getDataBase()!!.pointReference
        }

        fun getDataBase(): DataBase? {
            if (dataBase == null) {
                dataBase = DataBase()
            }
            return dataBase
        }
    }
}
