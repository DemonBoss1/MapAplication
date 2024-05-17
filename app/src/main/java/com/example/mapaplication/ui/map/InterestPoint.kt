package com.example.mapaplication.ui.map

import com.yandex.mapkit.geometry.Point

class InterestPoint() {
    lateinit var data: PlacemarkUserData
    lateinit var point: Point

    constructor(_data: PlacemarkUserData, _point: Point) : this() {
        data = _data
        point = _point
    }
}
