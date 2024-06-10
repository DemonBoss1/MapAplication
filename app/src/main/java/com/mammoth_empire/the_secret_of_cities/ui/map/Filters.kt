package com.mammoth_empire.the_secret_of_cities.ui.map

data class Filters(
    val isCafe: Boolean,
    val isHotel: Boolean,
    val isLandmark: Boolean
) {
    fun getsIntoFilter(data: PlacemarkUserData): Boolean{
        return (
                isCafe && data.type == PlacemarkType.CAFE
                ||
                isHotel && data.type == PlacemarkType.HOTEL
                ||
                isLandmark && data.type == PlacemarkType.ARCHITECTURE
                )
    }
}