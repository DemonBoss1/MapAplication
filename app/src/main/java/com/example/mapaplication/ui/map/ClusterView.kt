package com.example.mapaplication.ui.map

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.mapaplication.R

class ClusterView(context: Context?) : LinearLayout(context) {

    private val cafeText by lazy { findViewById<TextView>(R.id.text_cafe_pins) }
    private val landmarkText by lazy { findViewById<TextView>(R.id.text_landmark_pins) }
    private val hotelText by lazy { findViewById<TextView>(R.id.text_hotel_pins) }

    private val cafeLayout by lazy { findViewById<View>(R.id.layout_cafe_group) }
    private val landmarkLayout by lazy { findViewById<View>(R.id.layout_landmark_group) }
    private val hotelLayout by lazy { findViewById<View>(R.id.layout_hotel_group) }

    init {
        inflate(context, R.layout.cluster_view, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        orientation = HORIZONTAL
        setBackgroundResource(R.drawable.cluster_view_background)
    }

    fun setData(placemarkTypes: List<PlacemarkType>) {
        PlacemarkType.values().forEach {
            updateViews(placemarkTypes, it)
        }
    }

    private fun updateViews(
        placemarkTypes: List<PlacemarkType>,
        type: PlacemarkType
    ) {
        val (textView, layoutView) = when (type) {
            PlacemarkType.CAFE -> cafeText to cafeLayout
            PlacemarkType.ARCHITECTURE -> landmarkText to landmarkLayout
            PlacemarkType.HOTEL -> hotelText to hotelLayout
        }
        val value = placemarkTypes.countTypes(type)

        textView.text = value.toString()
        layoutView.isVisible = value != 0
    }

    private fun List<PlacemarkType>.countTypes(type: PlacemarkType) = count { it == type }
}