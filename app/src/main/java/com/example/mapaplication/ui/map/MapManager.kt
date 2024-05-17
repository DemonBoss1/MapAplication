package com.example.mapaplication.ui.map

import com.yandex.mapkit.Animation
import com.yandex.mapkit.ScreenPoint
import com.yandex.mapkit.ScreenRect
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView

class MapManager private constructor(private val mapView: MapView) {

    var isFocusRect = false

    fun init(){
        val map = mapView.mapWindow.map

        map.move(
            CameraPosition(
                Point(56.328101, 43.961775),
                /* zoom = */ 18.0f,
                /* azimuth = */ 305.0f,
                /* tilt = */ 60.0f
            ),
            Animation(Animation.Type.LINEAR, 1f),
            null
        )

    }


    companion object {
        private var mapManager: MapManager? = null

        fun get(mapView: MapView): MapManager?{
            if (mapManager == null)
                mapManager = MapManager(mapView)
            return mapManager
        }
        fun getFocusRect(): Boolean {
            return mapManager?.isFocusRect!!
        }

        fun updateFocusInfo(bottomPadding: Int){
            mapManager?.apply {
                mapView.mapWindow.focusRect = ScreenRect(
                    ScreenPoint(0f, 0f),
                    ScreenPoint(
                        mapView.mapWindow.width().toFloat(),
                        mapView.mapWindow.height().toFloat() - bottomPadding,
                    )
                )
                isFocusRect = true
            }
        }


    }

}