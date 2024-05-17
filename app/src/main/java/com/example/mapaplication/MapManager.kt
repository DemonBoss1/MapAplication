package com.example.mapaplication

import android.content.Context
import android.graphics.PointF
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mapaplication.databinding.FragmentMapBinding
import com.example.mapaplication.ui.map.MapFragment
import com.yandex.mapkit.Animation
import com.yandex.mapkit.ScreenPoint
import com.yandex.mapkit.ScreenRect
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.ClusterListener
import com.yandex.mapkit.map.ClusterTapListener
import com.yandex.mapkit.map.ClusterizedPlacemarkCollection
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.map.MapObjectDragListener
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkitdemo.objects.ClusterView
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.ui_view.ViewProvider

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