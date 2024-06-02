package com.example.mapaplication.ui.map

import com.yandex.mapkit.geometry.Point
import kotlinx.serialization.Serializable

@Serializable
data class HistoryItem(val point: PointForMap, val date: String, val nameHistoryPoint: String, val message: String?) {
}