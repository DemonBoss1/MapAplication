package com.example.mapaplication

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DataBase private constructor() {

    private val POINT_KEY = "InterestPoint"

    val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance("https://the-secret-of-cities-default-rtdb.europe-west1.firebasedatabase.app")
    val pointReference: DatabaseReference = firebaseDatabase.getReference(POINT_KEY)
    val pointList = arrayListOf<InterestPoint>()

    init {
        dataFromDB
    }

    private val dataFromDB: Unit
        get() {

            val valueEventListener: ValueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(pointList.isNotEmpty())pointList.clear();
                    for (ds in snapshot.children) {
                        val point = ds.getValue(InterestPoint::class.java)
                        if (point != null)
                            pointList.add(point)
                    }
                    if(isWait) MapManager.creatingPointInterest()
                    isWait = false
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            pointReference.addValueEventListener(valueEventListener)
        }

    companion object {
        private var dataBase: DataBase? = null
        var isWait = true
        fun updateFirebaseDatabase() {
            dataBase!!.dataFromDB
        }

        fun getDataBase(): DataBase? {
            if (dataBase == null) {
                dataBase = DataBase()
            }
            return dataBase
        }
    }
}
