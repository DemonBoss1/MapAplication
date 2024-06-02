package com.example.mapaplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PointF
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapaplication.databinding.ActivityMainBinding
import com.example.mapaplication.ui.history.HistoryFragment
import com.example.mapaplication.ui.map.ClusterView
import com.example.mapaplication.ui.map.Filters
import com.example.mapaplication.ui.map.InterestPoint
import com.example.mapaplication.ui.map.MapManager
import com.example.mapaplication.ui.map.Message
import com.example.mapaplication.ui.map.MessageAdapter
import com.example.mapaplication.ui.map.PlacemarkType
import com.example.mapaplication.ui.map.PlacemarkUserData
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
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.ui_view.ViewProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date

private const val CLUSTER_RADIUS = 60.0
private const val CLUSTER_MIN_ZOOM = 15

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val dataBase = DataBase.getDataBase()

    private lateinit var mapView: MapView
    private lateinit var mapManager: MapManager
    private lateinit var clasterizedCollection: ClusterizedPlacemarkCollection
    private var filters: Filters? = null

    val messageList = ArrayList<Message>()
    val adapter = MessageAdapter(messageList)

    var currentInterestPoint: InterestPoint? = null

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
                ClusterView(this).apply {
                    setData(placemarkTypes)
                }
            )
        )
        cluster.appearance.zIndex = 100f

        cluster.addClusterTapListener(clusterTapListener)
    }

    private val placemarkTapListener = MapObjectTapListener { mapObject, point ->

        val userData = mapObject.userData  as PlacemarkUserData

        SaveData.currentPointId = userData.id
        currentInterestPoint = InterestPoint(userData, point)

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

        DataBase.getDataBase()!!.messageReference.addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (ds in snapshot.children) {
                    val message = ds.getValue<Message>()
                    if (message != null && message.interestPointId == SaveData.currentPointId)
                        messageList.add(message)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Error loading message", Toast.LENGTH_LONG).show();
            }

        })

        true
    }

    private val getImageForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK){
            binding.userImage.setImageURI(result.data?.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = this
        MapKitFactory.setApiKey("8e68e3bf-421b-4ae6-ac02-7d71e41c9c36")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recordingUserName()

        val code = SaveData.pref.getString(SaveData.HistoryKeys, null)
        if (code != null) {
            val list : ArrayList<InterestPoint> = Json.decodeFromString<ArrayList<InterestPoint>>(code)
            Log.e("debugBegin", list[0].toString())
            if (list != null) {
                SaveData.historyPoints.addAll(list)
            }
        }

        init()
    }

    private fun init(){
        MapKitFactory.initialize(this)
        mapView = binding.mapview

        binding.apply {
            userImage.setOnClickListener {
                intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                getImageForResult.launch(intent)
            }

            closeMenuPoint.setOnClickListener {
                currentInterestPoint = null
                SaveData.currentPointId = null
                menuPoint.visibility = View.INVISIBLE
                messageSet.setText("")
            }
            sentMessage.setOnClickListener {
                if(messageSet.text.isNotEmpty()) {
                    val message = messageSet.text.toString()
                    messageSet.text.clear()

                    val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
                    val currentDate = sdf.format(Date())

                    if (SaveData.currentPointId != null) {
                        val mes = Message(
                            SaveData.currentPointId!!,
                            message,
                            SaveData.UserId,
                            currentDate
                        )
                        DataBase.getDataBase()!!.messageReference.push().setValue(mes)
                    }

                    adapter.notifyDataSetChanged()
                }
                SaveData.historyPoints?.add(currentInterestPoint!!)
                HistoryFragment.notifyDataSetChanged()
            }
            messages.layoutManager = LinearLayoutManager(this@MainActivity)
            messages.adapter = adapter

            closeMenuFilter.setOnClickListener {
                drawer.closeDrawer(GravityCompat.START)
                navView.selectedItemId = R.id.navigation_map
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

            }

            navView.selectedItemId = R.id.navigation_map
            navView.setOnItemSelectedListener {
                when(it.itemId){
                    R.id.navigation_filter -> {
                        drawer.closeDrawers()
                        drawer.openDrawer(GravityCompat.START)
                    }
                    R.id.navigation_map -> {
                        drawer.closeDrawers()
                    }
                    R.id.navigation_history -> {
                        drawer.closeDrawers()
                        drawer.openDrawer(GravityCompat.END)
                    }
                }
                true
            }
        }

        mapManager = MapManager.get(mapView)!!
        mapManager.init()
    }

    private fun recordingUserName(){
        SaveData.get(getSharedPreferences("setting", Context.MODE_PRIVATE))
        if(SaveData.pref.contains("username")){
            SaveData.username = SaveData.pref.getString("username", "").toString()
            SaveData.UserId = SaveData.pref.getString("ID", "").toString()
            binding.usernameMenu.visibility = View.GONE
        }
    }
    fun editName(view: View){
        val username = binding.editTextName.text.toString()
        SaveData.username = username
        val edit = SaveData.pref.edit()
        edit.putString("username",username).apply()

        val ref = DataBase.getDataBase()!!.userReference.push()
        SaveData.UserId = ref.key.toString()
        edit.putString("ID", SaveData.UserId).apply()

        DataBase.uploadImage(binding.userImage.drawable)

        binding.usernameMenu.visibility = View.GONE

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

        val edit = SaveData.pref.edit()
        val code = Json.encodeToString<ArrayList<InterestPoint>>(SaveData.historyPoints)
        Log.e("debugEnd", code.toString())
        edit.putString(SaveData.HistoryKeys,code).apply()

        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    companion object{
        private var mainActivity: MainActivity? = null

        fun creatingPointInterest() {
            MainActivity.mainActivity?.apply {
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
                            mainActivity,
                            R.drawable.cafe_ic
                        ),
                        PlacemarkType.ARCHITECTURE to ImageProvider.fromResource(
                            mainActivity,
                            R.drawable.landmark_icon
                        ),
                        PlacemarkType.HOTEL to ImageProvider.fromResource(
                            mainActivity,
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

                    clasterizedCollection.clusterPlacemarks(
                        CLUSTER_RADIUS,
                        CLUSTER_MIN_ZOOM
                    )

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