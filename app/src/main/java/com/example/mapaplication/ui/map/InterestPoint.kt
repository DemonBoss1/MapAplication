package com.example.mapaplication.ui.map

import android.os.Parcel
import android.os.Parcelable
import com.yandex.mapkit.geometry.Point
import kotlinx.serialization.Serializable

@Serializable
class InterestPoint() {
    lateinit var data: PlacemarkUserData
    lateinit var point: PointForMap

    constructor(_data: PlacemarkUserData, _point: Point) : this() {
        data = _data
        point = PointForMap(_point.latitude, _point.longitude)
    }

}
@Serializable
class PointForMap:Point{
    constructor() : super()
    constructor(latitude : Double, longitude  : Double) : super(latitude, longitude)
}
