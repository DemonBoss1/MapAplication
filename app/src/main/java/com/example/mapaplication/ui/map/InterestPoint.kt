package com.example.mapaplication.ui.map

import android.os.Parcel
import android.os.Parcelable
import com.yandex.mapkit.geometry.Point
import kotlinx.serialization.Serializable

class InterestPoint() {
    lateinit var data: PlacemarkUserData
    lateinit var point: Point

    constructor(_data: PlacemarkUserData, _point: Point) : this() {
        data = _data
        point = _point
    }

}
@Serializable
data class PointForHistory(val latitude : Double, val longitude : Double)
