package com.example.mapaplication.ui.map

import android.graphics.PointF
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapaplication.DataBase
import com.example.mapaplication.Filters
import com.example.mapaplication.MapManager
import com.example.mapaplication.Message
import com.example.mapaplication.MessageAdapter
import com.example.mapaplication.PlacemarkType
import com.example.mapaplication.PlacemarkUserData
import com.example.mapaplication.R
import com.example.mapaplication.Setting
import com.example.mapaplication.databinding.FragmentMapBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
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
import java.util.Date

private const val CLUSTER_RADIUS = 60.0
private const val CLUSTER_MIN_ZOOM = 15

class MapFragment : Fragment() {
    private lateinit var mapView: MapView
    private lateinit var mapManager: MapManager
    private lateinit var clasterizedCollection: ClusterizedPlacemarkCollection
    private var filters: Filters? = null

    private val dataBase = DataBase.getDataBase()
    private var _binding: FragmentMapBinding? = null
    val binding get() = _binding!!

    val messageList = ArrayList<Message>()
    val adapter = MessageAdapter(messageList)

    private val pinDragListener = object : MapObjectDragListener {
        override fun onMapObjectDragStart(p0: MapObject) {
        }

        override fun onMapObjectDrag(p0: MapObject, p1: Point) = Unit

        override fun onMapObjectDragEnd(p0: MapObject) {
            // Updates clusters position
            clasterizedCollection.clusterPlacemarks(CLUSTER_RADIUS, CLUSTER_MIN_ZOOM)
        }
    }

    private val clusterTapListener = ClusterTapListener {
        true
    }
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

    private val placemarkTapListener = MapObjectTapListener { mapObject, point ->

        val userData = mapObject.userData  as PlacemarkUserData

        Setting.currentPointId = userData.id

        binding.apply {
            menuPoint.visibility = View.VISIBLE
            titleMenuPoint.text = userData.title
            descriptionMenuPoint.text = userData.description
        }

        if (!MapManager.getFocusRect()) {
            MapManager.updateFocusInfo(binding.menuPoint.measuredHeight)
        }

        val cameraPosition = mapView.mapWindow.map.cameraPosition
        val zoom = cameraPosition.zoom
        val azimuth = cameraPosition.azimuth
        val tilt = cameraPosition.tilt
        val position = CameraPosition(
            point,
            zoom,
            azimuth,
            tilt
        )
        mapView.mapWindow.map.move(position)

        DataBase.getDataBase()!!.messageReference.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (ds in snapshot.children) {
                    val message = ds.getValue<Message>()
                    if (message != null && message.interestPointId == Setting.currentPointId)
                        messageList.add(message)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(activity, "Error loading message", Toast.LENGTH_LONG).show();
            }

        })

        true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mapFragment = this

        _binding = FragmentMapBinding.inflate(inflater, container, false)
        val root: View = binding.root

        init()

        return root
    }

    private fun init(){
        MapKitFactory.initialize(activity)
        mapView = binding.mapview

        binding.apply {
            closeMenuPoint.setOnClickListener {
                Setting.currentPointId = null
                menuPoint.visibility = View.INVISIBLE
                messageSet.setText("")
            }
            sentMessage.setOnClickListener {
                val message = messageSet.text.toString()
                messageSet.text.clear()

                val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
                val currentDate = sdf.format(Date())

                if(Setting.currentPointId!=null) {
                    val mes = Message(Setting.currentPointId!!, message, Setting.UserId, currentDate)
                    DataBase.getDataBase()!!.messageReference.push().setValue(mes)
                }

                adapter.notifyDataSetChanged()
            }
            messages.layoutManager = LinearLayoutManager(activity)
            messages.adapter = adapter

            filtersButton.setOnClickListener {
                filtersButton.visibility = View.INVISIBLE
                filterMenu.visibility = View.VISIBLE
            }
            closeMenuFilter.setOnClickListener {
                filtersButton.visibility = View.VISIBLE
                filterMenu.visibility = View.INVISIBLE

            }
            filterReset.setOnClickListener{
                checkBoxCafe.isChecked = false
                checkBoxHotel.isChecked = false
                checkBoxLandmark.isChecked = false

                resetFilter()
            }
            filterApply.setOnClickListener {

                val filter = Filters(
                    checkBoxCafe.isChecked,
                    checkBoxHotel.isChecked,
                    checkBoxLandmark.isChecked
                )
                setFilters(filter)

                filterApply()

                filtersButton.visibility = View.VISIBLE
                filterMenu.visibility = View.INVISIBLE

            }
        }

        mapManager = MapManager.get(mapView)!!
        mapManager.init()
    }
    fun setFilters(_filters: Filters){
        filters = _filters
    }

    fun resetFilter(){
        filters = null
    }
    fun filterApply(){
        clasterizedCollection.clear()
        creatingPointInterest()
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
    companion object {
        private lateinit var mapFragment: MapFragment
        fun creatingPointInterest() {
            mapFragment.apply {
                mapManager.apply {
                    val map = mapView.mapWindow.map

                    val points = dataBase!!.pointList

                    val collection = map.mapObjects.addCollection()

                    // Add a clusterized collection
                    clasterizedCollection =
                        collection.addClusterizedPlacemarkCollection(clusterListener)

                    // Add pins to the clusterized collection

                    val placemarkTypeToImageProvider = mapOf(
                        PlacemarkType.CAFE to ImageProvider.fromResource(
                            activity,
                            R.drawable.cafe_ic
                        ),
                        PlacemarkType.ARCHITECTURE to ImageProvider.fromResource(
                            activity,
                            R.drawable.landmark_icon
                        ),
                        PlacemarkType.HOTEL to ImageProvider.fromResource(
                            activity,
                            R.drawable.ic_hotel
                        ),
                    )
                    points.forEachIndexed { _, point ->
                        if (filters != null && !filters!!.getsIntoFilter(point.data))
                        else {
                            val type = point.data.type
                            val imageProvider = placemarkTypeToImageProvider[type] ?: return
                            clasterizedCollection.addPlacemark().apply {
                                geometry = point.point
                                setIcon(imageProvider, IconStyle().apply {
                                    anchor = PointF(0.5f, 1.0f)
                                    scale = 0.4f
                                })
                                // If we want to make placemarks draggable, we should call
                                // clasterizedCollection.clusterPlacemarks on onMapObjectDragEnd
                                isDraggable = true
                                setDragListener(pinDragListener)
                                // Put any data in MapObject
                                //val data = PlacemarkUserData("Data_$index","", PlacemarkType.ARCHITECTURE)
                                //val interestPoint = InterestPoint(data, point)
                                userData = point.data
                                addTapListener(placemarkTapListener)
                            }
                        }
                    }

                    clasterizedCollection.clusterPlacemarks(CLUSTER_RADIUS, CLUSTER_MIN_ZOOM)

//                val interestPoints = arrayListOf<InterestPoint>(
//                    InterestPoint(
//                        PlacemarkUserData(
//                            "",
//                            "Стадион",
//                            "Стадион «Нижний Новгород» — это многофункциональный спортивный комплекс, домашняя арена футбольного клуба «Пари Нижний Новгород» и один из лучших стадионов в мире.",
//                            PlacemarkType.ARCHITECTURE
//                        ),
//                        Point(
//                            56.337727,
//                            43.963353
//                        )
//                    )
//                )
//                interestPoints.forEach {interestPoint ->
//                    val ref = dataBase!!.pointReference.push()
//                    interestPoint.data.id=ref.key.toString()
//                    ref.setValue(interestPoint)
//                }
                }
            }
        }
    }
}
