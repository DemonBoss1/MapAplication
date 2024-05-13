package com.example.mapaplication

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import java.io.ByteArrayOutputStream

class DataBase private constructor() {

    private val POINT_KEY = "InterestPoint"
    private val USER_KEY = "USER"
    private val PICTURE_KEY = "ProfilePicture"

    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance("https://the-secret-of-cities-default-rtdb.europe-west1.firebasedatabase.app")
    private val pointReference: DatabaseReference = firebaseDatabase.getReference(POINT_KEY)
    val userReference: DatabaseReference = firebaseDatabase.getReference(USER_KEY)

    private val storage = FirebaseStorage.getInstance("gs://the-secret-of-cities.appspot.com")
    var storageRef = storage.getReference(PICTURE_KEY)

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
        fun uploadImage(drawable: Drawable){
            val bitMap = (drawable as BitmapDrawable).bitmap
            val baos = ByteArrayOutputStream()
            bitMap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val byteArray = baos.toByteArray()
            val mRef = dataBase?.storageRef?.child(Setting.ID)
            val up = mRef?.putBytes(byteArray)

        }
    }
}
