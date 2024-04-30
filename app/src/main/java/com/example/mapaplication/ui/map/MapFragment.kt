package com.example.mapaplication.ui.map

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mapaplication.PlacemarkType
import com.example.mapaplication.PlacemarkUserData
import com.example.mapaplication.R
import com.example.mapaplication.databinding.FragmentMapBinding
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
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

private const val CLUSTER_RADIUS = 60.0
private const val CLUSTER_MIN_ZOOM = 15
class MapFragment : Fragment() {
    private lateinit var mapView: MapView

    private var _binding: FragmentMapBinding? = null

    private lateinit var clasterizedCollection: ClusterizedPlacemarkCollection


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    //private val map = mapView.map

    private val clusterListener = ClusterListener { cluster ->
        val placemarkTypes = cluster.placemarks.map {
            (it.userData as PlacemarkUserData).type
        }
        // Sets each cluster appearance using the custom view
        // that shows a cluster's pins
        cluster.appearance.setView(
            ViewProvider(
                ClusterView(activity).apply {
                    setData(placemarkTypes)
                }
            )
        )
        cluster.appearance.zIndex = 100f

        cluster.addClusterTapListener(clusterTapListener)
    }
    private val clusterTapListener = ClusterTapListener {
        true
    }
    private val placeMarkTapListener = MapObjectTapListener { _, point ->
        Toast.makeText(
            activity,
            "Tapped the point (${point.longitude}, ${point.latitude})",
            Toast.LENGTH_SHORT
        ).show()
        true
    }
    private val pinDragListener = object : MapObjectDragListener {
        override fun onMapObjectDragStart(p0: MapObject) {
        }

        override fun onMapObjectDrag(p0: MapObject, p1: Point) = Unit

        override fun onMapObjectDragEnd(p0: MapObject) {
            // Updates clusters position
            clasterizedCollection.clusterPlacemarks(CLUSTER_RADIUS, CLUSTER_MIN_ZOOM)
        }
    }
    private val placemarkTapListener = MapObjectTapListener { mapObject, _ ->
        true
    }
    private val singlePlacemarkTapListener = MapObjectTapListener { _, _ ->
        true
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        val root: View = binding.root

        init()

        creatingPointInterest()

        return root
    }

    private fun init(){
        MapKitFactory.initialize(activity)
        mapView = binding.mapview
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
    private fun creatingPointInterest(){
        val map = mapView.mapWindow.map

        val points = listOf(
            Point(56.328324, 43.961277),
            Point(56.333727, 43.971358),
            Point(56.337533, 43.963401),
            Point(56.327306, 43.984995),
        )

        val collection = map.mapObjects.addCollection()

        // Add a clusterized collection
        clasterizedCollection = collection.addClusterizedPlacemarkCollection(clusterListener)

        // Add pins to the clusterized collection

        val placemarkTypeToImageProvider = mapOf(
            PlacemarkType.CAFE to ImageProvider.fromResource(activity, R.drawable.cafe_ic),
            PlacemarkType.ARCHITECTURE to ImageProvider.fromResource(activity, R.drawable.landmark_icon),
            PlacemarkType.HOTEL to ImageProvider.fromResource(activity, R.drawable.ic_hotel),
        )

        points.forEachIndexed { index, point ->
            val type = PlacemarkType.values().random()
            val imageProvider = placemarkTypeToImageProvider[type] ?: return
            clasterizedCollection.addPlacemark().apply {
                geometry = point
                setIcon(imageProvider, IconStyle().apply {
                    anchor = PointF(0.5f, 1.0f)
                    scale = 0.4f
                })
                // If we want to make placemarks draggable, we should call
                // clasterizedCollection.clusterPlacemarks on onMapObjectDragEnd
                isDraggable = true
                setDragListener(pinDragListener)
                // Put any data in MapObject
                userData = PlacemarkUserData("Data_$index", type)
                addTapListener(placemarkTapListener)
            }
        }

        clasterizedCollection.clusterPlacemarks(CLUSTER_RADIUS, CLUSTER_MIN_ZOOM)
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}