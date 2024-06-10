package com.mammoth_empire.the_secret_of_cities.ui.map

import kotlinx.serialization.Serializable

@Serializable
data class HistoryItem(val point: PointForHistory, val date: String, val nameHistoryPoint: String, val message: String?) {
}