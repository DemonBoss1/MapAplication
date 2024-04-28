package com.example.mapaplication.ui.home

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mapaplication.PlacemarkUserData
import com.example.mapaplication.R
import com.example.mapaplication.databinding.FragmentHomeBinding
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
import com.yandex.mapkit.map.TextStyle
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkitdemo.objects.ClusterView
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.ui_view.ViewProvider

private const val CLUSTER_RADIUS = 60.0
private const val CLUSTER_MIN_ZOOM = 15
class HomeFragment : Fragment() {
    private lateinit var mapView: MapView

    private var _binding: FragmentHomeBinding? = null

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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
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
        clasterizedCollection = collection.addClusterizedPlacemarkCollection(clusterListener)

        val imageProvider = ImageProvider.fromResource(activity, R.drawable.landmark_icon)

        points.forEach { point ->
            clasterizedCollection.addPlacemark().apply {
                geometry = point
                setIcon(imageProvider, IconStyle().apply {
                    anchor = PointF(0.5f, 1.0f)
                    scale = 0.6f
                })
                    // If we want to make placemarks draggable, we should call
                    // clasterizedCollection.clusterPlacemarks on onMapObjectDragEnd
                    isDraggable = true
                    setDragListener(pinDragListener)
                    // Put any data in MapObject
                    this.addTapListener(placemarkTapListener)
                }
        }

        clasterizedCollection.clusterPlacemarks(CLUSTER_RADIUS, CLUSTER_MIN_ZOOM)

        // Composite placemark with text
        val placemark = collection.addPlacemark(Point(56.327306, 43.984995)).apply {
            addTapListener(singlePlacemarkTapListener)
            // Set text near the placemark with the custom TextStyle

            setText(
                "Special place",
                TextStyle().apply {
                    size = 10f
                    placement = TextStyle.Placement.RIGHT
                    offset = 5f
                },
            )
        }

        placemark.useCompositeIcon().apply {
            // Combine several icons in the single composite icon
            setIcon(
                "point",
                ImageProvider.fromResource(activity, R.drawable.ic_circle),
                IconStyle().apply {
                    anchor = PointF(0.5f, 0.5f)
                    flat = true
                    scale = 0.05f
                }
            )
        }
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