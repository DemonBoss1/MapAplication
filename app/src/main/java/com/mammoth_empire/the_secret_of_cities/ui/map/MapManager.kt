package com.mammoth_empire.the_secret_of_cities.ui.map

import android.os.Environment
import android.util.Log
import com.yandex.mapkit.Animation
import com.yandex.mapkit.ScreenPoint
import com.yandex.mapkit.ScreenRect
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import java.io.File


class MapManager private constructor(private val mapView: MapView) {

    fun readFileAsLinesUsingUseLines(fileName: String): List<String> = File(fileName).useLines { it.toList() }
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
        Log.e("file", Environment.getExternalStorageDirectory().path)
        //val fin = File(Environment.getExternalStorageDirectory().path + "/" + File.separator + "Point.txt")
        //fin.createNewFile()
        //val stringList = readFileAsLinesUsingUseLines("Points.json")
//        Log.e("point",stringList[0])
//        Log.e("point",stringList[1])
//        Log.e("point",stringList[2])
//        Log.e("point",stringList[3])
//        Log.e("point",stringList[4])

    }


    companion object {
        private var mapManager: MapManager? = null

        fun get(mapView: MapView): MapManager?{
            if (mapManager == null)
                mapManager = MapManager(mapView)
            return mapManager
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
            }
        }
        fun movePosition(point: Point, zoom: Float) {
            val cameraPosition = mapManager!!.mapView.mapWindow.map.cameraPosition
            val azimuth = cameraPosition.azimuth
            val tilt = cameraPosition.tilt
            val newCameraPosition = CameraPosition(point, zoom, azimuth, tilt)
            mapManager?.mapView?.mapWindow?.map?.move(newCameraPosition)
        }


    }

}