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

private const val CLUSTER_RADIUS = 60.0
private const val CLUSTER_MIN_ZOOM = 15

class MapManager private constructor(private val activity: Context, private val binding: FragmentMapBinding) {
    private val dataBase = DataBase.getDataBase()

    private lateinit var mapView: MapView
    private lateinit var clasterizedCollection: ClusterizedPlacemarkCollection

    var isFocusRect = false

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
    private val placemarkTapListener = MapObjectTapListener { mapObject, point ->
        Toast.makeText(
            activity,
            "Tapped the point (${point.longitude}, ${point.latitude})",
            Toast.LENGTH_SHORT
        ).show()

        val userData = mapObject.userData  as PlacemarkUserData

        binding.apply {
            menuPoint.visibility = View.VISIBLE
            titleMenuPoint.text = userData.title
            descriptionMenuPoint.text = userData.description
        }

        if (!isFocusRect) {
            updateFocusInfo()
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

    fun updateFocusInfo(){
        val bottomPadding = binding.menuPoint.measuredHeight
        mapView.mapWindow.focusRect = ScreenRect(
            ScreenPoint(0f, 0f),
            ScreenPoint(
                mapView.mapWindow.width().toFloat(),
                mapView.mapWindow.height().toFloat() - bottomPadding,
            )
        )
        isFocusRect = true
    }

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

        fun get(fragment: Fragment): MapManager?{
            val fragmentMap = fragment as MapFragment
            val activity = fragmentMap.activity
            val binding = fragmentMap.binding
            if (mapManager == null)
                mapManager = activity?.let { MapManager(it, binding) }
            mapManager!!.mapView = binding.mapview

            return mapManager
        }
        fun creatingPointInterest() {
            mapManager?.apply {
                val map = mapView.mapWindow.map

                val points = dataBase!!.pointList

                val collection = map.mapObjects.addCollection()

                // Add a clusterized collection
                clasterizedCollection =
                    collection.addClusterizedPlacemarkCollection(clusterListener)

                // Add pins to the clusterized collection

                val placemarkTypeToImageProvider = mapOf(
                    PlacemarkType.CAFE to ImageProvider.fromResource(activity, R.drawable.cafe_ic),
                    PlacemarkType.ARCHITECTURE to ImageProvider.fromResource(
                        activity,
                        R.drawable.landmark_icon
                    ),
                    PlacemarkType.HOTEL to ImageProvider.fromResource(
                        activity,
                        R.drawable.ic_hotel
                    ),
                )
                if (points.isEmpty())
                    Log.e("point", "points.isEmpty")
                points.forEachIndexed { index, point ->
                    Log.w(
                        "point",
                        "Tapped the point (${point.point.longitude}, ${point.point.latitude})"
                    )
                    val type = point!!.data.type
                    val imageProvider = placemarkTypeToImageProvider[type] ?: return
                    clasterizedCollection.addPlacemark().apply {
                        Toast.makeText(
                            activity,
                            "Tapped the point (${index})",
                            Toast.LENGTH_SHORT
                        ).show()
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
                        val interestPoints = arrayListOf<InterestPoint>(
                            InterestPoint(PlacemarkUserData("Стадион", "Стадион «Нижний Новгород» — это многофункциональный спортивный комплекс, домашняя арена футбольного клуба «Пари Нижний Новгород» и один из лучших стадионов в мире.", PlacemarkType.ARCHITECTURE), Point(56.337727, 43.963353)),
                            InterestPoint(PlacemarkUserData("Кафедральный собор", "Кафедральный собор во имя Святого Благоверного Князя Александра Невского", PlacemarkType.ARCHITECTURE), Point(56.333621, 43.971271)),
                            InterestPoint(PlacemarkUserData("Нижегородская ярмарка", "Выставочный центр «Нижегородская ярмарка» — это место, где можно посетить множество выставок, концертов, фестивалей и других культурных мероприятий, а также купить сувениры. В зимнее время здесь работает ледяной лабиринт «Снежная королева», где можно прокатиться на ледяных горках.", PlacemarkType.ARCHITECTURE), Point(56.328300, 43.961199)),
                            InterestPoint(PlacemarkUserData("Нижегородский государственный цирк имени Маргариты Назаровой", "В Нижегородском государственном цирке им. Маргариты Назаровой можно увидеть выступления акробатов, воздушных гимнастов, дрессировщиков, клоунов, а также шоу с участием белых медведей, собак породы самоед и других животных.", PlacemarkType.ARCHITECTURE), Point(56.318548, 43.953339)),
                            InterestPoint(PlacemarkUserData("Архитектурно-этнографический музей-заповедник Щёлоковский хутор", "Этнографический парк-музей «Щёлоковский хутор» — это уникальное место, где можно погрузиться в атмосферу русской деревни XIX века. Здесь можно увидеть различные деревянные постройки, такие как избы, церкви, амбары и другие объекты, которые были восстановлены и отреставрированы.", PlacemarkType.ARCHITECTURE), Point(56.274019, 44.010644)),
                            InterestPoint(PlacemarkUserData("Нижегородский кремль", "Нижегородский кремль — это одна из главных достопримечательностей Нижнего Новгорода. Он был основан в XII веке и с тех пор претерпел множество изменений. В XVI веке крепость использовалась в качестве оборонительного сооружения, а в XVII веке стала административным центром губернии.", PlacemarkType.ARCHITECTURE), Point(56.328437, 44.003111)),
                            InterestPoint(PlacemarkUserData("Чкаловская лестница", "Чкаловская лестница, также известная как Волжская или Волжская лестница, является монументальным сооружением, расположенным в городе Нижний Новгород, Россия.", PlacemarkType.ARCHITECTURE), Point(56.330872, 44.009461)),
                            InterestPoint(PlacemarkUserData("Мужской монастырь", "Вознесенский Печерский мужской монастырь", PlacemarkType.ARCHITECTURE), Point(56.323073, 44.049733)),
                            InterestPoint(PlacemarkUserData("Marins Park Hotel Нижний Новгород", "Гостиница «Marins Park Hotel Нижний Новгород» расположена в Нижнем Новгороде, недалеко от железнодорожного вокзала.", PlacemarkType.HOTEL), Point(56.325373, 43.958653)),
                            InterestPoint(PlacemarkUserData("Никитин", "Гостиница «Никитин» расположена в старинном трехэтажном здании из красного кирпича на берегу слияния рек Волги и Оки, недалеко от кафедрального собора Александра Невского. Гостям предлагается проживание в просторных и комфортабельных номерах с высокими потолками и большим количеством окон.", PlacemarkType.HOTEL), Point(56.334846, 43.971025)),
                            InterestPoint(PlacemarkUserData("AZIMUT", "AZIMUT Отель Нижний Новгород расположен на высоком берегу реки Оки, откуда открывается великолепный вид на город и реку. Отель предлагает своим гостям номера различных категорий, включая полулюксы, люксы и видовые номера.", PlacemarkType.HOTEL), Point(56.323664, 43.980878)),
                            InterestPoint(PlacemarkUserData("Гранд Отель ОКА Премиум", "Гранд Отель «ОКА Премиум» — это четырехзвездочный отель, расположенный недалеко от парка «Швейцария». Он предлагает своим гостям комфортабельные номера с бесплатным доступом к сауне и бассейну, а также бесплатный фитнес-зал.", PlacemarkType.HOTEL), Point(56.293501, 43.979932)),
                            InterestPoint(PlacemarkUserData("Ланселот", "Кафе «Ланселот» — это уютное место, где можно провести время в приятной атмосфере, наслаждаясь вкусной едой и живой музыкой.", PlacemarkType.CAFE), Point(56.328966, 43.954154)),
                            InterestPoint(PlacemarkUserData("Ташир Пицца", "", PlacemarkType.CAFE), Point(56.318413, 43.925630)),
                            InterestPoint(PlacemarkUserData("Портер", "Бар «Портер», расположенный в Нижнем Новгороде, предлагает своим гостям широкий выбор крафтового пива, коктейлей, вин и настоек, а также бизнес-ланчи и блюда европейской кухни.", PlacemarkType.CAFE), Point(56.317321, 43.994375)),
                            InterestPoint(PlacemarkUserData("Патрон", "Ресторан «Патрон» специализируется на блюдах из мяса диких животных, таких как лось, косуля, кабан и пятнистый олень, а также на пельменях и строганине. В меню также есть рыба и десерты, такие как «Яблоко» и «Шишка».", PlacemarkType.CAFE), Point(56.330204, 44.020651)),
                            InterestPoint(PlacemarkUserData("Рога и Копыта", "«Рога и Копыта» — это бар, расположенный на улице Рождественской в Нижнем Новгороде.", PlacemarkType.CAFE), Point(56.326641, 43.981567)),
                        )
                        interestPoints.forEach {interestPoint ->
                            dataBase!!.pointReference.push().setValue(interestPoint)
                        }
                        userData = point.data
                        addTapListener(placemarkTapListener)
                    }
                }

                clasterizedCollection.clusterPlacemarks(CLUSTER_RADIUS, CLUSTER_MIN_ZOOM)
            }
        }
    }

}