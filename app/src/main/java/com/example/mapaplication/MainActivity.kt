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
import com.example.mapaplication.ui.map.HistoryItem
import com.example.mapaplication.ui.map.InterestPoint
import com.example.mapaplication.ui.map.MapManager
import com.example.mapaplication.ui.map.Message
import com.example.mapaplication.ui.map.MessageAdapter
import com.example.mapaplication.ui.map.PlacemarkType
import com.example.mapaplication.ui.map.PlacemarkUserData
import com.example.mapaplication.ui.map.PointForHistory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
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
        val cameraPosition = mapView.mapWindow.map.cameraPosition
        val zoom = cameraPosition.zoom
        val azimuth = cameraPosition.azimuth
        val tilt = cameraPosition.tilt
        val position = CameraPosition(
            /* target = */ it.appearance.geometry,
            /* zoom = */ zoom + 1,
            /* azimuth = */ azimuth,
            /* tilt = */ tilt
        )
        mapView.mapWindow.map.move(
            position,
            Animation(Animation.Type.LINEAR, 0.5f),
            null)

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

        MapManager.updateFocusInfo(binding.menuPoint.measuredHeight)

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
            val list : ArrayList<HistoryItem> = Json.decodeFromString<ArrayList<HistoryItem>>(code)
            SaveData.historyPoints.addAll(list)
        }

        init()

        //assets.open("Point.json")
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
                MapManager.updateFocusInfo(0)
                messageSet.setText("")
            }
            sentMessage.setOnClickListener {
                val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
                val currentDate = sdf.format(Date())

                var message: String? = null
                if(messageSet.text.isNotEmpty()) {
                    message = messageSet.text.toString()
                    messageSet.text.clear()

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
                val historyItem = HistoryItem(PointForHistory(currentInterestPoint!!.point.latitude, currentInterestPoint!!.point.longitude), currentDate, currentInterestPoint!!.data.title, message)
                SaveData.historyPoints.add(historyItem)
                HistoryFragment.notifyDataSetChanged()
            }
            messages.layoutManager = LinearLayoutManager(this@MainActivity)
            messages.adapter = adapter

            closeMenuFilter.setOnClickListener {
                drawer.closeDrawer(GravityCompat.START)
                navView.selectedItemId = R.id.navigation_map
            }
            filterSelectAll.setOnClickListener {
                checkBoxCafe.isChecked = true
                checkBoxHotel.isChecked = true
                checkBoxLandmark.isChecked = true
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
        val code = Json.encodeToString<ArrayList<HistoryItem>>(SaveData.historyPoints)
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
            mainActivity?.apply {
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
                    val interestPoints = arrayListOf<InterestPoint>(
                        InterestPoint(
                            PlacemarkUserData(
                                "",
                                "Сели съели",
                                "Кафе «Сели съели» — это место, где можно вкусно и сытно позавтракать, пообедать или поужинать. В меню представлены разнообразные блюда, такие как каши, запеканки, супы, мясные и рыбные блюда, гарниры, салаты, десерты и напитки. Кроме того, в кафе есть детская комната и стульчик для кормления, что делает его идеальным местом для семейного отдыха.",
                                PlacemarkType.CAFE
                            ),
                            Point(56.362391, 43.796594)
                        ),
                        InterestPoint(
                            PlacemarkUserData(
                                "",
                                "Трапеза",
                                "",
                                PlacemarkType.CAFE
                            ),
                            Point(56.359207, 43.793030)
                        ),
                        InterestPoint(
                            PlacemarkUserData(
                                "",
                                "Русская печка",
                                "",
                                PlacemarkType.CAFE
                            ),
                            Point(56.364046, 43.815565)
                        ),
                        InterestPoint(
                            PlacemarkUserData(
                                "",
                                "Моя Узола",
                                "",
                                PlacemarkType.CAFE
                            ),
                            Point(56.353869, 43.802781)
                        ),
                    )
                    val str =
                            "Калаш\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.346254, 43.805621\n" +
                            "\n" +
                            "Хан Кебаб\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.358613, 43.823430\n" +
                            "\n" +
                            "Светлояр\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.354973, 43.828767\n" +
                            "\n" +
                            "Шаурма\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.341524, 43.828282\n" +
                            "\n" +
                            "Шоти\n" +
                            "В ресторане «Шоти», расположенном в Нижнем Новгороде, можно отведать блюда грузинской, русской, азербайджанской и европейской кухонь. Гости отмечают, что здесь готовят хачапури «по-имеретински», хинкали и сациви, а также шашлык и люля-кебаб из баранины. Кроме того, посетители хвалят домашние гранатовые вина и чаи с этим фруктом. В меню также представлены салаты «Цезарь» и другие блюда, которые можно заказать на дом.\n" +
                            "CAFE\n" +
                            "56.367414, 43.845526\n" +
                            "\n" +
                            "Жара\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.364265, 43.852641\n" +
                            "\n" +
                            "На посошок\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.362334, 43.860783\n" +
                            "\n" +
                            "Старая Мельница\n" +
                            "Кафе «Старая Мельница» предлагает своим гостям блюда, приготовленные на открытом огне, такие как шашлык, стейки из лосося, люля кебаб и овощи на гриле. В кафе есть несколько беседок во внутреннем дворе, где можно провести время на свежем воздухе.\n" +
                            "CAFE\n" +
                            "56.359721, 43.868488\n" +
                            "\n" +
                            "Кафе\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.357355, 43.850844\n" +
                            "\n" +
                            "Граф\n" +
                            "Кафе «Граф» находится в Сормовском районе Нижнего Новгорода и предлагает своим гостям блюда европейской кухни. Интерьер заведения выполнен в брутальном стиле, напоминающем о рыцарских временах. В кафе есть несколько залов на разных этажах, включая банкетный зал, а также отдельные кабинки для компаний. Гостям нравится местная кухня, особенно жаркое «Купец второй гильдии» и семга, приготовленная на гриле. По выходным в кафе звучит живая музыка, а в меню есть бизнес-ланч и доставка еды.\n" +
                            "CAFE\n" +
                            "56.351122, 43.868066\n" +
                            "\n" +
                            "Киш Миш\n" +
                            "«Киш Миш» — это ресторан восточной кухни, расположенный в центре Сормова, недалеко от «Золотой мили». Он предлагает своим гостям разнообразное меню, включающее блюда среднеазиатской и грузинской кухонь. Посетители отмечают, что в ресторане царит уютная атмосфера, соответствующая восточному стилю, а также играет приятная музыка. Кроме того, гости высоко оценивают работу персонала, особенно официанта Ксению, которая быстро и качественно обслуживает посетителей.\n" +
                            "CAFE\n" +
                            "56.347468, 43.871702\n" +
                            "\n" +
                            "Villa Rosa\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.339402, 43.856111\n" +
                            "\n" +
                            "Salvador Dali\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.339652, 43.880011\n" +
                            "\n" +
                            "Варя\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.336391, 43.881421\n" +
                            "\n" +
                            "Сели съели\n" +
                            "«Сели съели» — это семейное кафе с широким выбором блюд и демократичными ценами. Гости отмечают, что здесь можно быстро и вкусно перекусить, а также взять еду с собой. В меню есть первые блюда, гарниры, мясные и рыбные блюда, салаты, десерты и выпечка.\n" +
                            "CAFE\n" +
                            "56.340238, 43.916107\n" +
                            "\n" +
                            "Каратэ Суши\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.330301, 43.915759\n" +
                            "\n" +
                            "Шашлык52\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.339369, 43.934256\n" +
                            "\n" +
                            "Соляная биржа\n" +
                            "Ресторан «Соляная биржа» предлагает своим гостям разнообразные блюда русской кухни, а также широкий выбор напитков, включая домашние лимонады и авторские настойки. В ресторане есть несколько залов, включая караоке-зал, VIP-зал и общий зал. Гости высоко оценивают кухню ресторана, особенно хвалят борщ, сома холодного копчения и тартар из овощей с муксуном.\n" +
                            "CAFE\n" +
                            "56.343181, 43.951238\n" +
                            "\n" +
                            "Базар\n" +
                            "Ресторан «Базар» славится своим уютным интерьером с мягкими диванами и приглушенным освещением, что делает его идеальным местом для семейных ужинов или встреч с друзьями. В ресторане подают блюда традиционной грузинской кухни, такие как «Хачапури по-аджарски», «Люля-кебаб», «Шашлык» и «Виттелло Тонато». Гости также рекомендуют попробовать «Том Ям», «Рамен с курицей», «Харчо», «Медовик» и другие блюда. Кроме того, ресторан предлагает бизнес-ланч и детское меню.\n" +
                            "CAFE\n" +
                            "56.340726, 43.957723\n" +
                            "\n" +
                            "Печь\n" +
                            "«Печь» — это пиццерия, расположенная в историческом здании с кирпичным сводчатым потолком и интерьером в стиле лофт. Здесь вы можете отведать вкуснейшую пиццу, приготовленную в настоящей дровяной печи, а также попробовать другие блюда, такие как супы, салаты, бургеры, ребрышки BBQ и десерты. Кроме того, в «Печи» представлен широкий выбор напитков, включая чай с имбирем, лимонад «Киви-Юдзу», «Лимон» и «Иван-чай», а также алкогольные напитки, такие как вино «Ежевика» и пиво от Горьковской пивоварни.\n" +
                            "CAFE\n" +
                            "56.332314, 43.956892\n" +
                            "\n" +
                            "Ланселот\n" +
                            "Кафе «Ланселот» — это уютное место, где можно провести время в приятной атмосфере, наслаждаясь вкусной едой и живой музыкой. Меню включает в себя разнообразные блюда, такие как борщ, паста карбонара, грибная лапша, солянка, фрикассе из курицы, овощи соте, спагетти болоньезе и другие. Кроме того, в кафе проводятся поминальные обеды.\n" +
                            "CAFE\n" +
                            "56.328966, 43.954154\n" +
                            "\n" +
                            "Мир Пиццы\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.326153, 43.957919\n" +
                            "\n" +
                            "Сим-Сим\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.329594, 43.938941\n" +
                            "\n" +
                            "Стейк-Хаус\n" +
                            "«Стейк-Хаус» — это ресторан, расположенный в гостинице «Marins Park Hotel». Он предлагает своим гостям широкий выбор блюд, включая супы-пюре, мясо и рыбу на гриле, а также десерты, приготовленные на глазах у посетителей. Гости особенно хвалят борщ, ростбиф, свинину от шеф-повара и десерты, такие как чизкейки и нью-йоркские чизкейки.\n" +
                            "CAFE\n" +
                            "56.325094, 43.958467\n" +
                            "\n" +
                            "Пита\n" +
                            "Кафе «Пита» — это небольшое, но уютное заведение, расположенное недалеко от Московского железнодорожного вокзала. В меню представлены блюда турецкой кухни, приготовленные на открытом огне, такие как шашлыки, саджи, искандеры и другие.\n" +
                            "CAFE\n" +
                            "56.322426, 43.950348\n" +
                            "\n" +
                            "Буфетъ\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.320718, 43.947359\n" +
                            "\n" +
                            "Шахзода\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.318137, 43.945991\n" +
                            "\n" +
                            "Руслан\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.316185, 43.952007\n" +
                            "\n" +
                            "Африка\n" +
                            "Кафе «Африка» — это тематическое заведение с интерьером, напоминающим африканскую саванну. Здесь можно провести любое мероприятие, будь то корпоратив, день рождения или поминальная трапеза. Гости отмечают вкусное и разнообразное меню, большие порции и красивую подачу блюд, а также внимательный и отзывчивый персонал. Кроме того, посетители хвалят музыкальное сопровождение и наличие танцпола.\n" +
                            "CAFE\n" +
                            "56.322141, 43.931424\n" +
                            "\n" +
                            "Абсолютъ\n" +
                            "Ресторан «Абсолютъ» находится в гостинице «Николь» и предлагает своим гостям разнообразное меню русской и зарубежной кухонь, а также постные блюда. В ресторане есть банкетные залы, которые могут вместить любое количество гостей. Кроме того, ресторан предоставляет скидку в размере 10% для гостей, проживающих в гостинице.\n" +
                            "CAFE\n" +
                            "56.321073, 43.929670\n" +
                            "\n" +
                            "Осетинские пироги\n" +
                            "Осетинские пироги — это кафе быстрого питания, где можно заказать осетинские пироги с различными начинками, такие как мясо, курица, капуста, вишня, малина и другие. Клиенты отмечают, что пироги здесь очень вкусные и сытные.\n" +
                            "CAFE\n" +
                            "56.319611, 43.925868\n" +
                            "\n" +
                            "Grill Food\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.313625, 43.934999\n" +
                            "\n" +
                            "Rostic's\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.299663, 43.947661\n" +
                            "\n" +
                            "Sushi-Star\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.311661, 43.925237\n" +
                            "\n" +
                            "Азия\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.316583, 43.898978\n" +
                            "\n" +
                            "Перекус\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.319186, 43.891457\n" +
                            "\n" +
                            "Кавказский дворик\n" +
                            "Кафе «Кавказский дворик» — это место, где можно провести время с друзьями, отметить день рождения или свадьбу, а также провести корпоративное мероприятие. Гости отмечают, что в кафе есть два зала, один из которых подходит для тихого отдыха, а другой — для танцев и живой музыки. В меню представлены блюда кавказской кухни, такие как шашлыки, люля-кебаб, овощи-гриль и хинкали, а также салаты и закуски. Кроме того, гости хвалят десерты, которые подаются в кафе.\n" +
                            "CAFE\n" +
                            "56.313829, 43.882174\n" +
                            "\n" +
                            "Новая столовая\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.304617, 43.871504\n" +
                            "\n" +
                            "Как дома\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.303466, 43.871344\n" +
                            "\n" +
                            "Шашлыковый период\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.319097, 43.867837\n" +
                            "\n" +
                            "Пауза\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.331241, 43.846273\n" +
                            "\n" +
                            "У Натика\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.289996, 43.848257\n" +
                            "\n" +
                            "Марико\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.287918, 43.848960\n" +
                            "\n" +
                            "Гурмет хаус\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.292164, 43.865227\n" +
                            "\n" +
                            "Князь\n" +
                            "В кафе «Князь» можно не только вкусно поесть, но и потанцевать. Гостям нравится местная кухня, особенно шашлык.\n" +
                            "CAFE\n" +
                            "56.289931, 43.886180\n" +
                            "\n" +
                            "Колизей\n" +
                            "«Колизей» — это кафе, которое подходит для различных мероприятий, таких как свадьбы, юбилеи, дни рождения и корпоративы. В кафе есть несколько залов, включая вип-зал с танцполом и живой музыкой, а также караоке-зал. Гости отмечают, что еда в «Колизее» вкусная и разнообразная, особенно хвалят шашлык, люля-кебаб, жульены и салат «Цезарь» с семгой. В отзывах также упоминается, что обслуживание в кафе вежливое и внимательное.\n" +
                            "CAFE\n" +
                            "56.289383, 43.902516\n" +
                            "\n" +
                            "Уют\n" +
                            "«Уют» — это кафе, где можно провести различные мероприятия, такие как свадьбы, юбилеи, корпоративы и поминальные обеды. Интерьер кафе выполнен в классическом стиле с использованием золотого декора, что делает его идеальным местом для проведения торжеств. Гостям нравится местная кухня, особенно шашлык, картофель и грибы, приготовленные на гриле, а также салаты «Цезарь» и жульены.\n" +
                            "CAFE\n" +
                            "56.295741, 43.939576\n" +
                            "\n" +
                            "Центр плова\n" +
                            "«Центр плова» — это кафе, которое специализируется на узбекской кухне.\n" +
                            "CAFE\n" +
                            "56.288959, 43.925242\n" +
                            "\n" +
                            "Легенда Чайхана\n" +
                            "«Легенда Чайхана» — это кафе, которое специализируется на узбекской кухне. Здесь можно попробовать разнообразные блюда, такие как лагман, плов, шашлык, манты, самса и многое другое.\n" +
                            "CAFE\n" +
                            "56.287685, 43.925342\n" +
                            "\n" +
                            "Хачапурия\n" +
                            "Ресторан «Хачапурия» — это место, где вы можете насладиться аутентичной грузинской атмосферой и попробовать классические грузинские блюда, такие как хачапури (грузинский хлеб с сыром) и хинкали (грузинские пельмени). В меню также есть лагман (густой суп с лапшой), долма (голубцы в виноградных листьях), харчо (суп с рисом и говядиной) и другие блюда.\n" +
                            "CAFE\n" +
                            "56.271771, 43.917021\n" +
                            "\n" +
                            "Dark Side\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.286260, 43.930602\n" +
                            "\n" +
                            "Самоваръ\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.271588, 43.938780\n" +
                            "\n" +
                            "Гурмет House\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.273078, 43.927093\n" +
                            "\n" +
                            "Welcome\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.252448, 43.938611\n" +
                            "\n" +
                            "Ванильный берег\n" +
                            "Кафе «Ванильный берег» — это место, где можно не только вкусно поесть, но и провести различные мероприятия, такие как свадьбы и банкеты. Гости отмечают красивый вид на реку Оку, который создает романтическую атмосферу, а также обширное меню и приветливый персонал.\n" +
                            "CAFE\n" +
                            "56.248067, 43.936364\n" +
                            "\n" +
                            "Шаурма по-турецки\n" +
                            "«Шаурма по-турецки» — это заведение быстрого питания, которое работает круглосуточно и предлагает широкий выбор блюд, включая шаурму. Клиенты отмечают, что здесь готовят очень вкусную шаурму с сочным мясом и свежими овощами, завернутыми в тонкий лаваш или французский батон. Кроме того, посетители хвалят айран — турецкий йогурт, который подается вместе с шаурмой. В заведении есть мягкие диваны и барные столики, а также удобная парковка.\n" +
                            "CAFE\n" +
                            "56.265344, 43.911683\n" +
                            "\n" +
                            "Мимино\n" +
                            "Кафе «Мимино» — это место, где вы можете насладиться домашней грузинской кухней, приготовленной из свежих и качественных ингредиентов.\n" +
                            "CAFE\n" +
                            "56.262209, 43.909815\n" +
                            "\n" +
                            "Кинза\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.265322, 43.893021\n" +
                            "\n" +
                            "Шашлык на Львовской\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.267238, 43.876998\n" +
                            "\n" +
                            "Эмир\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.258928, 43.884853\n" +
                            "\n" +
                            "Самурай\n" +
                            "Кафе «Самурай» — это уютное место с приятной атмосферой, где звучит ненавязчивая музыка. Гости отмечают, что персонал здесь очень приветливый и внимательный к каждому посетителю. В меню представлены блюда японской кухни, такие как суши, роллы и сашими, а также бургеры и пицца. Кроме того, здесь можно заказать бизнес-ланчи, которые пользуются популярностью у посетителей.\n" +
                            "CAFE\n" +
                            "56.243507, 43.864215\n" +
                            "\n" +
                            "Food King\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.250527, 43.851983\n" +
                            "\n" +
                            "Шашлык № 1\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.260561, 43.846220\n" +
                            "\n" +
                            "Автосуши Автопицца\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.226899, 43.823152\n" +
                            "\n" +
                            "Шашлык на углях у Гарика\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.235880, 43.832727\n" +
                            "\n" +
                            "Кафе Пельменная\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.223235, 43.836402\n" +
                            "\n" +
                            "Япоshik\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.218892, 43.839743\n" +
                            "\n" +
                            "Союз\n" +
                            "Ресторан «Союз» находится в Автозаводском районе, недалеко от парка, который, по словам посетителей, скоро будет благоустроен. В ресторане есть два этажа с уютными столиками, удобными диванами и креслами, а также барными стульями, откуда открывается панорамный вид. Гости отмечают, что интерьер ресторана выполнен в теплых тонах, а местная подсветка столов создает приятную атмосферу. В меню представлены разнообразные блюда, такие как роллы, гунканы, том ям, форель-гриль, кальмары и баклажаны, а также овощные брускеты.\n" +
                            "CAFE\n" +
                            "56.242232, 43.860038\n" +
                            "\n" +
                            "Dark Side\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.240279, 43.865418\n" +
                            "\n" +
                            "Кафе у озера\n" +
                            "Кафе «У Озера» — это место, где можно провести любое мероприятие, будь то день рождения, корпоратив или просто дружеские посиделки. В кафе есть несколько залов, поэтому вы можете выбрать тот, который лучше всего подходит для вашей компании. Гости отмечают, что здесь готовят очень вкусную еду, особенно хвалят шашлык. Кроме того, посетители отмечают вежливость и внимательность персонала, а также хорошую музыку и диджеев.\n" +
                            "CAFE\n" +
                            "56.235411, 43.852126\n" +
                            "\n" +
                            "Las Vegas\n" +
                            "Кафе Las Vegas — это отличное место для проведения различных мероприятий, таких как свадьбы, юбилеи, корпоративы и поминальные обеды. В кафе есть несколько залов, каждый из которых имеет отдельный вход, что позволяет проводить мероприятия без пересечений с другими компаниями. Интерьер залов выполнен в стиле 90-х годов, что создает атмосферу ностальгии. Гости отмечают, что кухня в кафе очень вкусная, особенно хвалят шашлык, люля кебаб, рыбу и овощи на гриле.\n" +
                            "CAFE\n" +
                            "56.230823, 43.848173\n" +
                            "\n" +
                            "Мангал Плюс\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.224498, 43.856843\n" +
                            "\n" +
                            "District\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.221710, 43.857918\n" +
                            "\n" +
                            "ПиццаФабрика\n" +
                            "Ресторан, кафе, пиццерия, доставка еды и обедов Ресторан «ПиццаФабрика» — это место, где можно провести время всей семьей. Здесь есть игровая комната с батутами, горками и лабиринтами, а также аниматоры, которые развлекают детей. Кроме того, в ресторане проводятся мастер-классы, где дети могут научиться готовить пиццу.\n" +
                            "CAFE\n" +
                            "56.234755, 43.870988\n" +
                            "\n" +
                            "Butch&Dutch\n" +
                            "Бар Butch&Dutch предлагает своим гостям широкий выбор пива, коктейлей и закусок, а также живую музыку и лазерное шоу. Посетители отмечают, что здесь можно провести время с друзьями, отметить день рождения или провести корпоративное мероприятие. Кроме того, бар имеет летнюю веранду с панорамным видом на реку Оку.\n" +
                            "CAFE\n" +
                            "56.216561, 43.866890\n" +
                            "\n" +
                            "Best food\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.217525, 43.839523\n" +
                            "\n" +
                            "Ла Веранда\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.207031, 43.802474\n" +
                            "\n" +
                            "Kebab Star\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.232916, 43.951437\n" +
                            "\n" +
                            "Автосуши Автопицца\n" +
                            "Суши-бар, кафе, ресторан Суши-бар «Автосуши и Автопицца» — это место, где вы можете насладиться разнообразными блюдами японской кухни, такими как суши, роллы и пицца, а также другими блюдами, такими как крем-суп, онигири и поке. В меню также есть комплексные обеды и бизнес-ланчи, которые можно заказать через мобильное приложение. Кроме того, в суши-баре есть детская комната, где ваши дети могут весело провести время, пока вы наслаждаетесь едой.\n" +
                            "CAFE\n" +
                            "56.230939, 43.946639\n" +
                            "\n" +
                            "Облака\n" +
                            "«Облака» — это кафе с современным интерьером и просторным танцполом. Они хвалят кухню, особенно шашлык и жульены, а также бизнес-ланчи по доступным ценам.\n" +
                            "CAFE\n" +
                            "56.223106, 43.940736\n" +
                            "\n" +
                            "Берёзовая Роща\n" +
                            "Кафе «Берёзовая Роща» — это место, где можно провести любое мероприятие, будь то свадьба, день рождения или корпоратив. Здесь есть несколько залов, которые могут вместить до 45 человек, и каждый из них имеет свой уникальный стиль. Например, зал «Белый» оформлен в классическом стиле, а зал «Дворцовый» — в дворцовом стиле с белыми стенами и золотыми элементами декора. На территории кафе есть парковка для автомобилей, а также фотозоны, где гости могут сделать красивые фотографии.\n" +
                            "CAFE\n" +
                            "56.221152, 43.937607\n" +
                            "\n" +
                            "Кафе\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.238340, 44.006168\n" +
                            "\n" +
                            "Столовая\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.240658, 43.990860\n" +
                            "\n" +
                            "5 Минут\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.241416, 43.977893\n" +
                            "\n" +
                            "Сладкий Горький\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.238047, 43.956414\n" +
                            "\n" +
                            "Natali\n" +
                            "Кафе Natali — это место, где можно не только вкусно поесть, но и весело провести время. Здесь можно отметить день рождения, провести корпоратив или просто отдохнуть с друзьями. В кафе есть как основной зал, так и летняя веранда.\n" +
                            "CAFE\n" +
                            "56.236433, 43.963116\n" +
                            "\n" +
                            "Есть Хочу\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.240564, 43.962946\n" +
                            "\n" +
                            "РаГу\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.244838, 43.967217\n" +
                            "\n" +
                            "Золотое кольцо\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.246613, 43.956238\n" +
                            "\n" +
                            "Спартак\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.255341, 43.968692\n" +
                            "\n" +
                            "Обжора\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.253555, 43.970651\n" +
                            "\n" +
                            "Мангал house\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.261701, 44.001516\n" +
                            "\n" +
                            "Шаурма в Дубëнках\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.258442, 43.998814\n" +
                            "\n" +
                            "Шашлычный дворик\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.248370, 43.986554\n" +
                            "\n" +
                            "НаОбед\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.245453, 43.988068\n" +
                            "\n" +
                            "Горячие обеды\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.281537, 43.981240\n" +
                            "\n" +
                            "Пицца Марвел\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.280689, 43.995901\n" +
                            "\n" +
                            "Визит\n" +
                            "Кафе «Визит» — это уютное место, где можно провести различные мероприятия, такие как юбилеи, дни рождения, корпоративы и бизнес-ланчи. Гости отмечают вкусную еду, красивую сервировку столов и приветливость персонала. Они также отмечают, что в кафе можно принести свой алкоголь и напитки, а также выбрать музыку на свой вкус.\n" +
                            "CAFE\n" +
                            "56.275065, 43.989226\n" +
                            "\n" +
                            "СВ-кафе\n" +
                            "Семейное кафе СВ-кафе — это место, где вы можете насладиться домашней кухней и уютной атмосферой.\n" +
                            "CAFE\n" +
                            "56.274142, 43.978848\n" +
                            "\n" +
                            "Та самая шаурма на Средном\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.296444, 43.976083\n" +
                            "\n" +
                            "Elleven\n" +
                            "Ресторан Elleven находится в отеле «Гранд-Ока» на одиннадцатом этаже, откуда открывается прекрасный вид на город. Гостям нравится разнообразное меню с интересными авторскими блюдами из рыбы, а также стейки из мраморной говядины. Особенно хвалят судака горячего копчения с припущенным шпинатом в сливках и салат с копченым угрем и авокадо. Живая музыка и шоу-программы с участием барменов также привлекают посетителей.\n" +
                            "CAFE\n" +
                            "56.293878, 43.979085\n" +
                            "\n" +
                            "Буфетъ\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.293198, 43.978224\n" +
                            "\n" +
                            "Матрешка\n" +
                            "Ресторан «Матрешка» предлагает своим гостям блюда русской кухни в аутентичном интерьере, оформленном в народном стиле. Здесь можно попробовать борщ с малиной, уху со сливками, шашлык из свиных щек, судака, запеченного в картофельном гратене, драники, блины, оливье и многое другое. Также в ресторане есть детское меню и комната для детей. Гости отмечают, что цены в ресторане доступные, а обслуживание быстрое и ненавязчивое.\n" +
                            "CAFE\n" +
                            "56.289152, 43.982317\n" +
                            "\n" +
                            "Суши-бар\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.290422, 43.994366\n" +
                            "\n" +
                            "Sushi-Star\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.284525, 43.993478\n" +
                            "\n" +
                            "City\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.292943, 44.014860\n" +
                            "\n" +
                            "Сербский Гриль\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.296048, 44.033045\n" +
                            "\n" +
                            "Sehr Gut!\n" +
                            "Sehr Gut! — это ресторан с баварской атмосферой, расположенный недалеко от центра города. Гостям нравится местная кухня и пиво, которое варится на собственной пивоварне, а также настойки из можжевельника.\n" +
                            "CAFE\n" +
                            "56.288892, 44.036548\n" +
                            "\n" +
                            "BrosFood\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.283388, 44.034716\n" +
                            "\n" +
                            "Домашняя Италия\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.295751, 44.040607\n" +
                            "\n" +
                            "Островок\n" +
                            "Кафе «Островок» идеально подходит для проведения банкетов и дней рождения, так как здесь есть уютная атмосфера и обширное меню. Гости особенно хвалят шашлык и люля-кебаб, а также рекомендуют попробовать фирменное армянское мороженое и лимонад.\n" +
                            "CAFE\n" +
                            "56.288269, 44.044888\n" +
                            "\n" +
                            "Печёрский дворик\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.299491, 44.067990\n" +
                            "\n" +
                            "Pizza Ricca\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.296357, 44.065107\n" +
                            "\n" +
                            "Il Tempo\n" +
                            "Il Tempo — это ресторан итальянской кухни, расположенный недалеко от торгового центра Хофф в Нижнем Новгороде. В ресторане царит уютная атмосфера, играет итальянская музыка, а на столах стоят зажженные свечи. Гости рекомендуют попробовать фирменное блюдо ресторана — баклажаны в хрустящей корочке, салат с тунцом, пасту в сырной «голове», пиццу с курицей и грибами, а также пиццу с грушей. Из напитков советуют попробовать лимонад, облепиховый чай, настойку «Лимончелло» и вино, которое порекомендует официант.\n" +
                            "CAFE\n" +
                            "56.293455, 44.076042\n" +
                            "\n" +
                            "Ацумари\n" +
                            "Ресторан японской кухни «Ацумари» предлагает своим гостям широкий выбор блюд, включая роллы, суши, сашими и морепродукты. Гости особенно рекомендуют попробовать роллы «Филадельфия» и «Техас», а также том-ям и рамен. Интерьер ресторана выполнен в минималистичном стиле с мягкими диванами и перегородками, отделяющими столики друг от друга. В ресторане есть открытая кухня и парковка.\n" +
                            "CAFE\n" +
                            "56.304434, 44.079092\n" +
                            "\n" +
                            "Брынза\n" +
                            "Ресторан «Брынза» предлагает своим гостям блюда грузинской кухни. Гости отмечают, что особенно им нравятся хинкали, люля-кебаб и жареный сыр, а также сыры, приготовленные в собственной сыроваренной. Кроме того, посетители хвалят лимонады, которые подаются в кувшинах, и травяные чаи. Из напитков также рекомендуют попробовать облепиховый чай с десертом.\n" +
                            "CAFE\n" +
                            "56.306155, 44.075983\n" +
                            "\n" +
                            "Печёра\n" +
                            "Столовая «Печёра» — это место, где можно не только вкусно поесть, но и провести различные мероприятия, такие как свадьбы, юбилеи и поминальные обеды. Здесь вы найдете широкий выбор блюд, включая салаты, супы, выпечку, десерты и многое другое. Кроме того, они хвалят работу персонала, который всегда вежлив и готов помочь с выбором блюд. В столовой есть два зала, один из которых оформлен в стиле блинной, а также летняя терраса.\n" +
                            "CAFE\n" +
                            "56.317137, 44.060532\n" +
                            "\n" +
                            "Колос\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.310761, 44.036787\n" +
                            "\n" +
                            "Кушать подано\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.318875, 44.037485\n" +
                            "\n" +
                            "Пронто пицца и паста\n" +
                            "Кафе «Пронто пицца и паста» предлагает своим гостям блюда итальянской кухни, такие как пицца, паста, салаты, супы и десерты. Посетители особенно хвалят грибной крем-суп, салат «Цезарь» с креветками, пиццу «Адриатика» и десерт «Тирамису».\n" +
                            "CAFE\n" +
                            "56.317633, 44.018993\n" +
                            "\n" +
                            "Nevazhno\n" +
                            "Кафе Nevazhno предлагает своим гостям блюда европейской и японской кухни, а также широкий выбор алкогольных и безалкогольных напитков, включая авторские коктейли и кальяны. Из окон кафе открывается прекрасный вид на город с высоты 6-го этажа. Кроме того, в кафе проводятся вечеринки с диджеями и танцами.\n" +
                            "CAFE\n" +
                            "56.316927, 44.020050\n" +
                            "\n" +
                            "Мясо Есть\n" +
                            "«Мясо Есть» — это ресторан, который специализируется на мясных блюдах. Здесь вы можете попробовать различные виды стейков, такие как миньон, рибай, филе-миньон и т.д. Также в меню есть бургеры с курицей, говяжьи стейки на гриле, салат «Цезарь» и другие блюда.\n" +
                            "CAFE\n" +
                            "56.307604, 44.023707\n" +
                            "\n" +
                            "Сова\n" +
                            "Ресторан «Сова» расположен в тихом районе, недалеко от центра города. Он предлагает своим гостям вкусные завтраки в формате «шведского стола», а также бизнес-ланчи и ужины по меню. Кроме того, в отеле есть сауна и джакузи, а также подземный паркинг.\n" +
                            "CAFE\n" +
                            "56.302417, 44.029228\n" +
                            "\n" +
                            "Мама Дома\n" +
                            "В ресторане «Мама Дома» царит уютная домашняя атмосфера, которая располагает к приятному отдыху в кругу семьи или друзей. Гости отмечают, что интерьер заведения продуман до мелочей: на каждом столике есть настольные игры, такие как домино, а на стенах висят забавные цитаты великих людей о еде, которые можно прочитать в ожидании заказа.\n" +
                            "CAFE\n" +
                            "56.316798, 44.007714\n" +
                            "\n" +
                            "Biblioteca\n" +
                            "Biblioteca — это уютное кафе с необычным интерьером, оформленным цитатами знаменитых писателей. Гостям нравится местная кухня, особенно они рекомендуют попробовать пасту, салаты, стейки и десерты, такие как чизкейки и меренговые рулеты.\n" +
                            "CAFE\n" +
                            "56.317143, 43.995292\n" +
                            "\n" +
                            "Самурай\n" +
                            "«Самурай» — это кафе, расположенное в центре города, с красивым видом из окон на Большую Покровскую улицу. Гостям нравится разнообразное меню, включающее в себя роллы, пиццу, гавайскую пиццу, сырные палочки, домашние лимонады, коктейли, а также детское меню и бизнес-ланч. Кроме того, посетители отмечают приятную атмосферу, вежливое и быстрое обслуживание, а также наличие дисконтной программы.\n" +
                            "CAFE\n" +
                            "56.316141, 43.993517\n" +
                            "\n" +
                            "Ле гриль\n" +
                            "Ресторан «Ле гриль» предлагает своим гостям изысканные блюда, приготовленные из высококачественных ингредиентов, а также обширную винную карту. Гостям особенно понравились стейки, ребрышки и говядина «Веллингтон», а также десерты, такие как «Павлова» и «Медовик». В ресторане также можно заказать бизнес-ланч, который включает в себя суп, салат, стейк и бокал шампанского. Кроме того, гости высоко оценили оригинальную подачу блюд и внимательный персонал.\n" +
                            "CAFE\n" +
                            "56.310476, 44.000256\n" +
                            "\n" +
                            "Та самая шаурма на Средном\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.309433, 43.991188\n" +
                            "\n" +
                            "Баренц\n" +
                            "Ресторан «Баренц» специализируется на блюдах из рыбы и морепродуктов, а также на устрицах и морских ежах. В меню представлены разнообразные закуски, салаты, супы, горячие блюда и десерты. Гости рекомендуют попробовать салат «Нисуаз», борщ, гребешки, роллы «Филадельфия», уху, треску в кисло-сладком соусе, мидии в соусе «Горгонзола», кальмаров с розмарином, осьминогов и другие блюда. Кроме того, в ресторане можно заказать пиво, водку, коктейли и вина, включая «Пино Гриджио».\n" +
                            "CAFE\n" +
                            "56.311837, 43.990756\n" +
                            "\n" +
                            "Горячий Пельмень\n" +
                            "«Горячий Пельмень» — это кафе, расположенное недалеко от площади Горького и станции метро. Это место славится своими домашними пельменями и варениками с различными начинками, такими как курица, индейка, щука, шпинат и сыр, а также грибами и вишней. В меню также есть супы, салаты и напитки, такие как лимонад «Ностальгия» и «Облепиховый морс». Интерьер кафе уютный и оформлен зеленью, что создает приятную атмосферу.\n" +
                            "CAFE\n" +
                            "56.315750, 43.987292\n" +
                            "\n" +
                            "Тануки\n" +
                            "Ресторан «Тануки» предлагает своим гостям блюда японской и европейской кухни, а также большой выбор напитков, включая бамбуковое пиво. Гости отмечают, что здесь готовят очень вкусные суши и роллы, такие как «Филадельфия», «Чеддер» и «Юмико», а также рекомендуют попробовать салат «Синсиро» с острыми креветками и кальмарами, рис «Тяхан» с морепродуктами и десерт «Три молока».\n" +
                            "CAFE\n" +
                            "56.313552, 43.989437\n" +
                            "\n" +
                            "Monica\n" +
                            "Monica — это итальянский ресторан, расположенный в торговом центре Небо. Он славится своей вкусной пиццей, приготовленной в дровяной печи, а также салатами, супами и горячими блюдами. Гости также высоко оценивают десерты, такие как меренговые рулеты и джелато. Кроме того, ресторан предлагает широкий выбор напитков, включая коктейли и настойки.\n" +
                            "CAFE\n" +
                            "56.309387, 43.986686\n" +
                            "\n" +
                            "Хачапури & Вино\n" +
                            "Ресторан «Хачапури & Вино» — это место, где можно насладиться аутентичной грузинской кухней и вином, а также провести время в уютной атмосфере. Гости отмечают, что персонал здесь очень вежливый и внимательный, а обслуживание быстрое и качественное.В меню представлены разнообразные блюда, такие как хачапури (аджарский, по-мегрельски и по-имеретински), хинкали (жареные и отварные), чебуреки, шашлыки и многое другое.\n" +
                            "CAFE\n" +
                            "56.308482, 43.987613\n" +
                            "\n" +
                            "АндерСон\n" +
                            "Ресторан «АндерСон» — это идеальное место для семейного отдыха. Здесь есть две детские комнаты, в которых работают аниматоры, которые присмотрят за детьми, чтобы они не ссорились. Кроме того, в ресторане проводятся мастер-классы для детей, где они могут научиться чему-то новому и интересному. Родители могут расслабиться и насладиться вкусной едой, пока их дети играют. В ресторане также есть специальное детское меню и акции на десерты.\n" +
                            "CAFE\n" +
                            "56.314361, 43.983294\n" +
                            "\n" +
                            "Селедка и кофе\n" +
                            "Кафе «Селедка и кофе» находится в историческом центре Нижнего Новгорода на улице Рождественской. Интерьер заведения выполнен в европейском стиле и напоминает хороший бар. Гостям нравится местная кухня, особенно форшмак, тартар из говядины, мидии в пасте, картофельные дольки с беконом, бургеры и креветки, а также напитки, такие как кровавая Мэри, лавандовый лимонад, чай Пуэр и настойки.\n" +
                            "CAFE\n" +
                            "56.329782, 43.994232\n" +
                            "\n" +
                            "Соус\n" +
                            "Кафе «Соус» — это уютное городское кафе, расположенное напротив Речного вокзала, с террасой и летней верандой с удобными креслами-качелями, откуда открывается прекрасный вид на исторический центр Нижнего Новгорода. В меню представлены разнообразные блюда, такие как томленая говядина с свежими овощами, борщ с томленой говядиной, салат из авокадо и креветок с индийскими специями, крабовый салат, хашбрауны, бриоши, брускеты с различными начинками и многое другое.\n" +
                            "CAFE\n" +
                            "56.328195, 43.987913\n" +
                            "\n" +
                            "Red Wall\n" +
                            "Red Wall — это ресторан авторской кухни, расположенный у стен Нижегородского Кремля. Из окон и с летней веранды открывается прекрасный вид на реку Волгу и башню Кремля. В ресторане есть собственная винодельня, где гости могут попробовать вина, произведенные из тосканских сортов винограда. Меню ресторана предлагает широкий выбор блюд, включая уху, приготовленную на углях, пельмени из судака, тартар из оленя, ростбиф, фаршированных перепелов и гребешков, а также десерты, такие как лавандовое облако.\n" +
                            "CAFE\n" +
                            "56.330930, 44.002846\n" +
                            "\n" +
                            "Пяткин\n" +
                            "Ресторан «Пяткин» — это аутентичное русское заведение, расположенное в старинном купеческом доме. Гостям нравится местная кухня, особенно борщ с гречневой кашей, уха из петуха с расстегаями, ассорти из соленых черных груздей, пельмени из щуки и судак с грибным соусом. Кроме того, посетители хвалят грибной суп в хлебной тарелке, пироги с брусникой и крендели, а также фирменные настойки — смородиновую, анисовую и хреновую. В ресторане звучит негромкая музыка, а официанты подают блюда очень быстро.\n" +
                            "CAFE\n" +
                            "56.329505, 43.992358\n" +
                            "\n" +
                            "Тюбетейка\n" +
                            "Ресторан «Тюбетейка» — это одно из старейших заведений города, которое предлагает своим гостям блюда узбекской кухни. Гости отмечают, что в ресторане царит уютная и загадочная атмосфера, а также играет приятная музыка. В ресторане есть несколько залов с мягкими диванами и креслами, удобными широкими столами и потолочными сводами из кирпичей с тканевыми навесами. Также в ресторане есть мини-фонтан, туалетные комнаты с теплой водой из-под крана, кондиционеры с теплым воздухом и кальяны.\n" +
                            "CAFE\n" +
                            "56.327348, 43.983741\n" +
                            "\n" +
                            "Цейлон\n" +
                            "Ресторан «Цейлон» — это стилизованное заведение, расположенное в Нижнем Новгороде, которое специализируется на блюдах индийской кухни. В меню представлены разнообразные блюда, такие как карри, масала, чатни, а также вегетарианские и веганские блюда. Гостям особенно понравились баклажаны в темпуре с томатным муссом, куриные котлеты из ланкийского тунца, обжаренная цветная капуста с карри, лепешки роти и наан, а также кокосовый пудинг «Ваталаппан» и десерт из блинчиков с матчей и кокосовым кремом.\n" +
                            "CAFE\n" +
                            "56.323547, 44.001307\n" +
                            "\n" +
                            "Закат\n" +
                            "Кафе «Закат» находится на набережной реки Волги, откуда открывается прекрасный вид на достопримечательности города, такие как Чкаловская лестница и памятник «Герой-катер». Интерьер кафе выполнен в современном стиле, с удобными стульями и большими панорамными окнами, из которых открывается потрясающий вид на реку. В меню представлены разнообразные блюда, включая салаты, супы, шашлыки, рыбу и морепродукты, а также авторские чаи и десерты.\n" +
                            "CAFE\n" +
                            "56.332152, 44.010291\n" +
                            "\n" +
                            "Лепи Тесто\n" +
                            "«Лепи Тесто» — это кафе, расположенное недалеко от Нижегородского кремля, которое специализируется на пельменях и варениках. В меню представлены различные виды пельменей с необычными начинками, такими как оленина, индейка и лосось, а также классические варианты, такие как «карбонара» и «Россия». Кроме того, в кафе можно заказать салаты, супы и напитки, включая чай, кофе и пиво. Интерьер кафе выполнен в уникальном стиле с использованием книг на полках и сувенирных носков на доске.\n" +
                            "CAFE\n" +
                            "56.328514, 44.015435\n" +
                            "\n" +
                            "Джани ресторани\n" +
                            "«Джани ресторани» — это ресторан грузинской кухни, расположенный в центре Нижнего Новгорода. Он предлагает своим гостям разнообразное меню, в котором можно найти блюда на любой вкус. Посетители особенно хвалят хачапури, долму, хинкали и мини-чебуреки, а также салаты «Солнце Тбилиси» и «Городской завтрак». В ресторане проводятся различные мероприятия, такие как дни рождения, свадьбы и корпоративы. Кроме того, здесь есть летняя веранда и собственная парковка.\n" +
                            "CAFE\n" +
                            "56.326549, 44.012069\n" +
                            "\n" +
                            "Mitrich Steakhouse\n" +
                            "Ресторан Mitrich Steakhouse — это идеальное место для деловых обедов или ужинов. Здесь можно попробовать стейки различных степеней прожарки. Также в меню есть мидии, тартар из сига, борщ, телячья щека и другие блюда. Из напитков — алкогольные и безалкогольные коктейли, а также вино. Кроме того, в ресторане есть детская комната.\n" +
                            "CAFE\n" +
                            "56.323244, 44.011736\n" +
                            "\n" +
                            "Синдбад\n" +
                            "«Синдбад» — это ресторан восточной кухни, расположенный на красивой набережной, с которой открывается великолепный вид на просторы Волги. Интерьер ресторана выполнен в эклектичном стиле, сочетающем в себе элементы восточной и западной культур. Гости высоко оценивают местную кухню, отмечая, что здесь можно найти блюда на любой вкус: от индийского супа «харчо» до узбекских пельменей «чучвара» и баранины в соусе «карри».\n" +
                            "CAFE\n" +
                            "56.329085, 44.018016\n" +
                            "\n" +
                            "Крабс\n" +
                            "«Крабс» — это ресторан, специализирующийся на блюдах из морепродуктов. Гости отмечают, что в меню есть большой выбор устриц, креветок, фаланг камчатских крабов и других морских деликатесов. Кроме того, посетители рекомендуют попробовать дораду, запеченных устриц и салаты с крабами и авокадо. Ресторан предлагает бизнес-ланчи и основное меню, а также большой выбор вин и натуральных лимонадов. Интерьер ресторана выполнен в спокойных тонах, а обслуживание быстрое и вежливое.\n" +
                            "CAFE\n" +
                            "56.325001, 44.023389\n" +
                            "\n" +
                            "Барелли\n" +
                            "Ресторан «Барелли» предлагает своим гостям широкий выбор блюд итальянской кухни, включая пасту, пиццу, салаты, супы и десерты. В ресторане также есть своя пекарня, где можно приобрести свежую выпечку и хлеб. Кроме того, в ресторане проводятся бесплатные мастер-классы для детей по воскресеньям.\n" +
                            "CAFE\n" +
                            "56.323052, 44.030344\n" +
                            "\n" +
                            "Балатон\n" +
                            "Кафе «Балатон» — это венгерское заведение, расположенное недалеко от канатной дороги.\n" +
                            "CAFE\n" +
                            "56.322973, 44.037225\n" +
                            "\n" +
                            "Монастырская чайная Паломник\n" +
                            "\n" +
                            "CAFE\n" +
                            "56.323363, 44.047250\n" +
                            "\n" +
                            "Беркут\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.389439, 43.736897\n" +
                            "\n" +
                            "Гостевой дом на Заболотной\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.388357, 43.773280\n" +
                            "\n" +
                            "Евразия\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.379061, 43.803862\n" +
                            "\n" +
                            "HomeHotel\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.360980, 43.801054\n" +
                            "\n" +
                            "Мона Лиза\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.370573, 43.842694\n" +
                            "\n" +
                            "Balmont\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.361898, 43.840490\n" +
                            "\n" +
                            "ДзенХоум Спокойствие 93\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.359440, 43.851738\n" +
                            "\n" +
                            "Никитин\n" +
                            "Гостиница «Никитин» расположена в старинном трехэтажном здании из красного кирпича, в историческом центре города. Гостям предлагается проживание в просторных номерах с высокими потолками и мансардными окнами, откуда открывается великолепный вид на город. В каждом номере есть ванная комната с душем, халатами, тапочками, феном и бесплатными туалетно-косметическими принадлежностями. Кроме того, гости могут воспользоваться гладильной доской и утюгом, которые предоставляются по запросу.\n" +
                            "HOTEL\n" +
                            "56.334846, 43.971025\n" +
                            "\n" +
                            "Marins Park Hotel Нижний Новгород\n" +
                            "Гостиница «Marins Park Hotel Нижний Новгород» предлагает своим гостям комфортное проживание в просторных номерах, оформленных в современном стиле. Каждый номер оснащен телевизором с плазменным экраном, кондиционером, феном и бесплатными туалетно-косметическими принадлежностями, а также тапочками и одноразовыми туалетными принадлежностями. Кроме того, в номерах есть мини-бассейн и сауна, которые можно забронировать за дополнительную плату.\n" +
                            "HOTEL\n" +
                            "56.325373, 43.958653\n" +
                            "\n" +
                            "Нижний\n" +
                            "Гостиница «Нижний» расположена в центре города, недалеко от станции метро и железнодорожного вокзала, а также в нескольких минутах ходьбы от автобусной остановки. Отель предлагает своим гостям номера различных категорий, оформленные в стиле «лофт». Все номера оснащены чайником, микроволновой печью, плитой, посудой, чаем, сахаром, халатами, тапочками и бесплатными туалетно-косметическими принадлежностями. Некоторые номера имеют мини-кухню с микроволновой печью и посудой.\n" +
                            "HOTEL\n" +
                            "56.320286, 43.950269\n" +
                            "\n" +
                            "Майский сад\n" +
                            "Гостиница «Майский сад» расположена в тихом районе, недалеко от Московского вокзала. Гостям предлагается проживание в просторных номерах с холодильником, чайником, телевизором, кондиционером и гигиеническими принадлежностями, а также тапочками. В отеле есть кафе, где подают домашнюю еду по доступным ценам.\n" +
                            "HOTEL\n" +
                            "56.314731, 43.932418\n" +
                            "\n" +
                            "Закат\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.305109, 43.925291\n" +
                            "\n" +
                            "КапитоLinn\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.352065, 43.871069\n" +
                            "\n" +
                            "Машина избушка\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.331174, 43.917867\n" +
                            "\n" +
                            "Каскад\n" +
                            "Гостиница «Каскад» расположена в Сормовском районе Нижнего Новгорода и предлагает своим гостям комфортабельные номера различных категорий, включая полулюксы и люксы.\n" +
                            "HOTEL\n" +
                            "56.349382, 43.871260\n" +
                            "\n" +
                            "Надежда\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.328014, 43.877944\n" +
                            "\n" +
                            "Московская\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.328561, 43.856222\n" +
                            "\n" +
                            "Марлен\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.308870, 43.859007\n" +
                            "\n" +
                            "Маяк\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.341534, 43.825696\n" +
                            "\n" +
                            "Орион\n" +
                            "Гостиница «Орион» расположена на выезде из города, недалеко от остановки общественного транспорта и продуктовых магазинов «Пятерочка» и «Авокадо». К услугам гостей номера с кондиционером, бесплатным Wi-Fi, кабельным телевидением, холодильником, микроволновой печью, чайником и принадлежностями для чая/кофе.\n" +
                            "HOTEL\n" +
                            "56.310692, 43.813937\n" +
                            "\n" +
                            "Гостиница\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.308910, 43.754326\n" +
                            "\n" +
                            "Старый замок\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.308585, 43.727832\n" +
                            "\n" +
                            "Лесная сказка\n" +
                            "«Лесная сказка» — это гостиница, оформленная в стиле языческой лесной избы, с лепниной и головами животных на стенах. В номерах есть мини-холодильник, телевизор, фен, гель для душа и шампунь. На первом этаже гостиницы находится уютное кафе, где подают блюда русской кухни, такие как щи, жаркое из печи и салат «Восточная сказка». Завтрак включен в стоимость номера, а также есть возможность заказать обед или ужин.\n" +
                            "HOTEL\n" +
                            "56.308056, 43.715443\n" +
                            "\n" +
                            "Мотель Камил\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.306900, 43.700717\n" +
                            "\n" +
                            "Солар\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.292023, 43.840745\n" +
                            "\n" +
                            "Maxx Royal\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.289363, 43.888024\n" +
                            "\n" +
                            "Мансарда\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.287272, 43.910900\n" +
                            "\n" +
                            "Заречная\n" +
                            "Гостиница «Заречная» расположена недалеко от станции метро и торгового центра, а также в нескольких минутах ходьбы от кинотеатра и катка. Она предлагает своим гостям номера с двуспальными кроватями, холодильниками, телевизорами и кондиционерами. Кроме того, в каждом номере есть чайник и чайные принадлежности, а также ванная комната с душем и туалетом. Гости также могут воспользоваться бильярдной комнатой и бесплатной парковкой.\n" +
                            "HOTEL\n" +
                            "56.284982, 43.929620\n" +
                            "\n" +
                            "SV\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.272721, 43.923849\n" +
                            "\n" +
                            "Славянка\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.266514, 43.926481\n" +
                            "\n" +
                            "Максидом\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.231527, 43.870690\n" +
                            "\n" +
                            "Волна\n" +
                            "Гостиница «Волна» предлагает своим гостям комфортное проживание в номерах с удобными кроватями, белоснежным постельным бельем и полотенцами всех размеров. В каждом номере есть телевизор, мини-бар и фен, а также туалетные принадлежности, включая пилочки для ногтей. Кроме того, гости могут воспользоваться услугами тренажерного зала, бильярдной комнаты и настольного тенниса. На территории гостиницы есть бесплатная парковка.\n" +
                            "HOTEL\n" +
                            "56.243200, 43.868965\n" +
                            "\n" +
                            "Автозаводская\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.242400, 43.861596\n" +
                            "\n" +
                            "Апартаменты Vegas на Молодёжном Проспекте 31/1\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.241600, 43.847920\n" +
                            "\n" +
                            "Уют\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.244445, 43.829656\n" +
                            "\n" +
                            "Uloo на Спутника\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.238580, 43.827454\n" +
                            "\n" +
                            "Багира\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.231052, 43.831657\n" +
                            "\n" +
                            "Атлантик\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.223310, 43.836579\n" +
                            "\n" +
                            "Апарт-отель на Малышевской 109\n" +
                            "В апарт-отеле «На Малышевской» гостям предлагается проживание в просторных номерах, оснащенных всем необходимым для комфортного отдыха. В каждом номере есть холодильник, чайник и микроволновая печь, а также собственная ванная комната.\n" +
                            "HOTEL\n" +
                            "56.213806, 43.834745\n" +
                            "\n" +
                            "Старая мельница\n" +
                            "«Старая мельница» — это уютная гостиница с номерами, оформленными в деревенском стиле, где есть все необходимое для комфортного проживания. Гости отмечают, что номера оснащены кондиционерами, телевизорами, холодильниками, душевыми и туалетными комнатами, а также халатами, тапочками и индивидуальными средствами гигиены. Кроме того, в номерах есть просторные кровати, что делает их идеальным местом для отдыха после долгой дороги.\n" +
                            "HOTEL\n" +
                            "56.236034, 43.736267\n" +
                            "\n" +
                            "Гостиный двор\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.211902, 43.763220\n" +
                            "\n" +
                            "Гостиница Стригино\n" +
                            "Гостиница «Стригино» расположена в живописном месте, окруженном сосновым лесом, недалеко от реки Оки. Она предлагает своим гостям комфортабельные номера с большими кроватями и просторными ванными комнатами. В стоимость проживания включен завтрак, который можно выбрать из трех вариантов: каша, омлет или блинчики с джемом и йогуртом. Кроме того, гости могут насладиться вкусным обедом или ужином в ресторане гостиницы.\n" +
                            "HOTEL\n" +
                            "56.195540, 43.774670\n" +
                            "\n" +
                            "Ной\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.211431, 43.928009\n" +
                            "\n" +
                            "Argos\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.217741, 43.933362\n" +
                            "\n" +
                            "Калитка\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.239142, 43.992832\n" +
                            "\n" +
                            "Лес&парк\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.269871, 43.997219\n" +
                            "\n" +
                            "Золотой Клевер\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.276993, 44.023943\n" +
                            "\n" +
                            "Астра\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.300873, 44.030013\n" +
                            "\n" +
                            "Саврасовская\n" +
                            "Гостиница «Саврасовская» расположена на третьем этаже здания и предлагает своим гостям просторные номера с высокими потолками. В каждом номере есть ванная комната с душевой кабиной и туалетом, а также мини-кухня с чайником и микроволновой печью.\n" +
                            "HOTEL\n" +
                            "56.295682, 44.019280\n" +
                            "\n" +
                            "ЛогХаус\n" +
                            "Гостиница «ЛогХаус» предлагает своим гостям комфортабельные номера с мини-кухнями, холодильниками, плитами, чайниками и посудой, а также стильными мангалами и удобными широкими столами. Кроме того, гости могут посетить сауну «Сакура» и баню «Зубр», а также арендовать беседки для проведения различных мероприятий, таких как корпоративы или дни рождения. На территории гостиницы также есть пруд и многоуровневый ландшафтный дизайн.\n" +
                            "HOTEL\n" +
                            "56.298697, 44.003684\n" +
                            "\n" +
                            "Гранд Отель ОКА Премиум\n" +
                            "«Гранд Отель ОКА Премиум» — это четырехзвездочный отель, расположенный в центре города Нижний Новгород. Он предлагает своим гостям комфортабельные номера, ресторан, конференц-залы, боулинг, бильярд и фитнес-центр с бассейном. Кроме того, гости могут посетить сауну и массажный кабинет. На территории отеля есть бесплатная парковка.\n" +
                            "HOTEL\n" +
                            "56.293501, 43.979932\n" +
                            "\n" +
                            "Hampton by Hilton Nizhny Novgorod\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.320425, 44.029743\n" +
                            "\n" +
                            "Октябрьская\n" +
                            "Гостиница «Октябрьская» расположена на высоком берегу реки Волги, откуда открывается прекрасный вид на город и реку. В гостинице есть номера различных категорий, включая люксы и апартаменты, а также ресторан на первом этаже. В номерах есть все необходимое для комфортного проживания, включая чайник, чайные принадлежности, холодильник и телевизор. Гостям также предоставляются тапочки, полотенца и средства личной гигиены.\n" +
                            "HOTEL\n" +
                            "56.328540, 44.019674\n" +
                            "\n" +
                            "История\n" +
                            "Гостиница «История» расположена в историческом центре Новгорода, в непосредственной близости от Кремля и набережной Волги. Гостям предлагается проживание в уютных номерах, оснащенных кондиционерами, холодильниками, фенами и полотенцесушителями. В каждом номере есть две бутылки воды, а на первом этаже можно бесплатно воспользоваться кулером, чаем, чашками и местом для отдыха. Кроме того, на территории гостиницы есть бесплатная парковка.\n" +
                            "HOTEL\n" +
                            "56.330665, 43.999467\n" +
                            "\n" +
                            "Пешков Отель\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.325883, 44.010653\n" +
                            "\n" +
                            "Славия\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.323930, 44.009583\n" +
                            "\n" +
                            "Salstory\n" +
                            "Гостиница Salstory расположена в историческом центре Нижнего Новгорода, в нескольких минутах ходьбы от главных достопримечательностей города, таких как Нижегородский кремль и набережная Волги. Гостям предлагается проживание в просторных номерах с двуспальными кроватями и диванами, оформленных в современном стиле. В каждом номере есть чайник, кофе и чай, а также все необходимые туалетно-косметические принадлежности, включая зубную щетку и тапочки.\n" +
                            "HOTEL\n" +
                            "56.329477, 43.993644\n" +
                            "\n" +
                            "Багет\n" +
                            "Гостиница «Багет» расположена в самом сердце Нижнего Новгорода, откуда открывается великолепный вид на Кремль и реку Волгу. Гостям предлагается размещение в уютных номерах, оснащенных всем необходимым для комфортного проживания: телевизором, холодильником, чайником, феном и тапочками. В каждом номере есть отдельная ванная комната с душем и туалетом, а также мини-кухня с микроволновой печью и чайником. На территории гостиницы есть бесплатная парковка.\n" +
                            "HOTEL\n" +
                            "56.328633, 43.994626\n" +
                            "\n" +
                            "Минин\n" +
                            "Гостиница «Минин» расположена в самом сердце Нижнего Новгорода, всего в нескольких минутах ходьбы от Нижегородского Кремля и пешеходной улицы Покровки. Она предлагает своим гостям комфортабельные номера с балконом, откуда открывается вид на город или реку. В каждом номере есть ванная комната с феном и бесплатными туалетно-косметическими принадлежностями, а также мини-бар с чаем, кофе и водой. Кроме того, в номерах есть тапочки, халат и набор для бритья. Завтрак «шведский стол» включен в стоимость номера и сервируется каждое утро в ресторане гостиницы.\n" +
                            "HOTEL\n" +
                            "56.324360, 43.999420\n" +
                            "\n" +
                            "Sheraton\n" +
                            "Гостиница Sheraton расположена в историческом центре Нижнего Новгорода, недалеко от Кремля и пешеходной улицы Большая Покровская. Гостям предлагается проживание в просторных номерах с удобными кроватями и белоснежным постельным бельем. В номерах есть все необходимое для комфортного проживания, включая гладильную доску, утюг, чайный столик и мини-бар с напитками. По утрам в клубном лаундже на крыше отеля проходят завтраки с живой музыкой и шампанским.\n" +
                            "HOTEL\n" +
                            "56.324665, 44.001722\n" +
                            "\n" +
                            "Mercure Нижний Новгород Центр\n" +
                            "Гостиница Mercure Нижний Новгород Центр предлагает своим гостям комфортное проживание в просторных номерах, оснащенных холодильником, чайником, феном, телевизором, а также полотенцами и гигиеническими принадлежностями. В каждом номере есть ванная комната с душем, туалетом и бесплатными туалетно-косметическими принадлежностями, такими как шампунь, мыло и кондиционер. Кроме того, в номерах есть чайник с чаем, кофе, сливками и сахаром.\n" +
                            "HOTEL\n" +
                            "56.321994, 44.003022\n" +
                            "\n" +
                            "Joy\n" +
                            "Гостиница Joy расположена в самом центре Нижнего Новгорода, всего в нескольких минутах ходьбы от Кремля и пешеходной улицы Большая Покровская. К услугам гостей номера с кондиционером, холодильником, чайником и микроволновой печью, а также бесплатный Wi-Fi. В некоторых номерах есть балкон с мини-кухней и гостиным уголком. Каждое утро в номер подают завтрак.\n" +
                            "HOTEL\n" +
                            "56.322288, 43.998932\n" +
                            "\n" +
                            "Ibis\n" +
                            "Гостиница Ibis расположена в Нижнем Новгороде и предлагает своим гостям комфортабельные номера с холодильником, телевизором, шкафом и тапочками. Гости могут насладиться вкусным завтраком «шведский стол» с омлетами, кашами, овощами, картофелем, сыром, хлопьями, сырниками и круассанами, а также молочными коктейлями для детей. Кроме того, в отеле есть ресторан, где подают блюда французской кухни.\n" +
                            "HOTEL\n" +
                            "56.315059, 44.001798\n" +
                            "\n" +
                            "Кулибин\n" +
                            "Гостиница «Кулибин» предлагает своим гостям комфортное проживание в просторных номерах, оформленных в современном европейском стиле. В номерах есть все необходимое для комфортного отдыха, включая халаты, тапочки, принадлежности для душа и кофе-машину. Кроме того, в гостинице есть спа-центр с сауной, бассейном, джакузи и хаммамом, а также ресторан, где можно попробовать блюда местной кухни.\n" +
                            "HOTEL\n" +
                            "56.315339, 44.005110\n" +
                            "\n" +
                            "Волга\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.327608, 43.986750\n" +
                            "\n" +
                            "Мегаполис\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.327023, 43.982712\n" +
                            "\n" +
                            "Марко\n" +
                            "Гостиница «Марко» расположена в исторической части Нижнего Новгорода, недалеко от Кремля и Речного вокзала. Она предлагает своим гостям уютные номера с новой мебелью и сантехникой, а также бесплатный Wi-Fi. В каждом номере есть кондиционер, телевизор, чайник, холодильник, фен и бесплатные туалетно-косметические принадлежности. Некоторые номера имеют балкон с видом на реку. Завтрак подается в номер по предварительному заказу.\n" +
                            "HOTEL\n" +
                            "56.325803, 43.981200\n" +
                            "\n" +
                            "AZIMUT Отель Нижний Новгород\n" +
                            "AZIMUT Отель Нижний Новгород расположен на высоком берегу Оки, откуда открывается великолепный вид на город, реку и Стрелку, где сливаются Волга и Ока. В отеле есть номера различных категорий, включая полулюксы, люксы и номера с видом на реку. Все номера оснащены чайником и набором для чая/кофе, а также бесплатными туалетно-косметическими принадлежностями, халатами и тапочками. Завтрак «шведский стол» включен в стоимость проживания и сервируется с 07:30 до 11:00.\n" +
                            "HOTEL\n" +
                            "56.323664, 43.980878\n" +
                            "\n" +
                            "Seven\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.324141, 43.993963\n" +
                            "\n" +
                            "Парус\n" +
                            "Гостиница «Парус» расположена в историческом центре города и предлагает своим гостям комфортабельные номера с холодильником, телевизором, кондиционером, шкафом, кроватями, столом и пуфом, а также халатами, тапочками и чайным набором. В некоторых номерах есть окно с видом на город. Гости могут насладиться завтраком «шведский стол», который включает в себя каши, омлеты, запеканки, блины, сосиски и гарниры, а также овощи, печенье и чай или кофе. Кроме того, гости могут посетить сауну и хаммам за дополнительную плату.\n" +
                            "HOTEL\n" +
                            "56.322573, 43.989818\n" +
                            "\n" +
                            "Кортъярд Нижний Новгород Сити центр\n" +
                            "Гостиница «Кортъярд Нижний Новгород Сити центр» расположена в центре города и предлагает своим гостям комфортабельные номера с просторными ванными комнатами. В номерах есть все необходимые удобства, такие как чайник, чайные принадлежности, утюг и гладильная доска. Кроме того, гости могут насладиться вкусным завтраком, который подается в формате «шведского стола».\n" +
                            "HOTEL\n" +
                            "56.322413, 43.992165\n" +
                            "\n" +
                            "9 Ночей\n" +
                            "Гостиница «9 Ночей» расположена в центре Нижнего Новгорода, в шаговой доступности от главных достопримечательностей города, таких как Кремль, Чкаловская лестница и улица Большая Покровская. Гостям предлагается размещение в уютных номерах-студиях с двуспальной кроватью и раскладным диваном.\n" +
                            "HOTEL\n" +
                            "56.311796, 43.977766\n" +
                            "\n" +
                            "Александровский сад\n" +
                            "Гостиница «Александровский сад» расположена на набережной Нижнего Новгорода, откуда открывается прекрасный вид на реку Волгу. Отель предлагает своим гостям комфортабельные номера с просторными кроватями, халатами, тапочками, полотенцами и бесплатными туалетно-косметическими принадлежностями. Кроме того, в номерах есть кофемашина и мини-бар, а также бесплатный доступ к сауне, бассейну, тренажерному залу и боулингу. Завтрак подается в формате «шведского стола» с большим выбором блюд, включая слабосоленую рыбу. Гостям также нравится ресторан отеля, где подают блюда по умеренным ценам.\n" +
                            "HOTEL\n" +
                            "56.329872, 44.023147\n" +
                            "\n" +
                            "Дипломат\n" +
                            "Гостиница «Дипломат» расположена в центре Нижнего Новгорода, недалеко от Кремля и основных достопримечательностей города. В каждом номере есть чайник, чай и кофе, а также мини-бар и сейф. В ванной комнате предоставляются бесплатные туалетно-косметические принадлежности, включая зубную щетку, шампунь и гель для душа. Кроме того, гости могут насладиться вкусным и разнообразным завтраком «шведский стол».\n" +
                            "HOTEL\n" +
                            "56.325208, 44.023727\n" +
                            "\n" +
                            "Премьер\n" +
                            "Гостиница «Премьер» расположена в тихом районе, недалеко от центра города. Номера оснащены всем необходимым для комфортного проживания: халатами, тапочками, электрическим чайником, кулером с водой, чаем, кофе и водой. В каждом номере также есть телевизор, фен и кондиционер. Гости могут выбрать один из двух видов завтрака: английский с овсянкой или русский с гречкой и молоком. Кроме того, в гостинице есть ресторан, где подают вкусные блюда.\n" +
                            "HOTEL\n" +
                            "56.313817, 44.031383\n" +
                            "\n" +
                            "ЛофТ Отель\n" +
                            "Отель «ЛофТ Отель» расположен в тихом районе, недалеко от центра города. Он предлагает своим гостям комфортабельные номера с бесплатным Wi-Fi и круглосуточным доступом к чаю и кофе. Кроме того, в отеле есть общая кухня, где гости могут приготовить себе еду.\n" +
                            "HOTEL\n" +
                            "56.319027, 44.060451\n" +
                            "\n" +
                            "Easy Room\n" +
                            "Гостиница Easy Room расположена в тихом районе Нижнего Новгорода, недалеко от торгового центра и фитнес-центра. Она предлагает своим гостям уютные номера с новой мебелью и свежим постельным бельем. В каждом номере есть кондиционер, телевизор с приставкой и бесплатный Wi-Fi, а также ванная комната с душем и туалетом. Кроме того, гости могут насладиться завтраком, который включает в себя несколько вариантов на выбор, включая йогурт, приготовленный на месте.\n" +
                            "HOTEL\n" +
                            "56.313519, 44.066798\n" +
                            "\n" +
                            "12 Месяцев\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.313324, 44.073856\n" +
                            "\n" +
                            "Агродом\n" +
                            "\n" +
                            "HOTEL\n" +
                            "56.304442, 44.037491\n" +
                            "\n" +
                            "Арт 11\n" +
                            "Гостиница «Арт 11» расположена в тихом районе Нижнего Новгорода, недалеко от оживленных магистралей и торговых центров. Она предлагает своим гостям уютные номера с теплыми полами, кондиционерами, холодильниками и телевизорами, а также стильными ванными комнатами с наборами для душа и фенами. В номерах также есть столики, полки для хранения вещей и урны, а в некоторых номерах есть балконы. Гостям предоставляется бесплатный завтрак, который состоит из каши, яиц, сосисок, запеканки и чая. Кроме того, гости могут воспользоваться микроволновой печью и чайником в номере.\n" +
                            "HOTEL\n" +
                            "56.288626, 44.081152\n" +
                            "\n" +
                            "Гранат\n" +
                            "Гостиница «Гранат» предлагает своим гостям комфортное проживание в уютных номерах, оформленных в современном стиле. В номерах есть все необходимое для комфортного проживания, включая зубные наборы, тапочки и Wi-Fi.\n" +
                            "HOTEL\n" +
                            "56.276571, 44.083722\n" +
                            "\n" +
                            "Церковь Тихвинской иконы Божией Матери\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.333724, 43.617746\n" +
                            "\n" +
                            "Шуховская пожарная башня\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.380347, 43.831052\n" +
                            "\n" +
                            "Церковь Всех Святых\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.360038, 43.790887\n" +
                            "\n" +
                            "Церковь иконы Божией Матери Нечаянная Радость\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.359292, 43.823200\n" +
                            "\n" +
                            "Церковь святого праведного воина Феодора Ушакова\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.342212, 43.811252\n" +
                            "\n" +
                            "Церковь Троицы Живоначальной\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.330901, 43.823551\n" +
                            "\n" +
                            "Музей истории завода Красное Сормово\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.359953, 43.870081\n" +
                            "\n" +
                            "Церковь Александра Невского\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.357886, 43.869049\n" +
                            "\n" +
                            "Водосброс\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.354143, 43.896140\n" +
                            "\n" +
                            "Собор Преображения Господня в Сормове\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.349862, 43.872040\n" +
                            "\n" +
                            "Назад в СССР\n" +
                            "Музей Назад в СССР — это уникальное место, где можно погрузиться в атмосферу Советского Союза. Здесь вы найдете множество экспонатов, воссоздающих быт того времени, от автомобилей и мотоциклов до телевизоров и магнитофонов. Посетители отмечают, что это место напоминает им о счастливом детстве, и они с радостью возвращаются сюда снова и снова.\n" +
                            "ARCHITECTURE\n" +
                            "56.334293, 43.855105\n" +
                            "\n" +
                            "Лимпопо\n" +
                            "Зоопарк «Лимпопо» является первым частным зоопарком в России, основанным в 2003 году в городе Нижний Новгород. На его территории площадью более 30 гектаров находится более 230 различных видов животных, таких как тигры, жирафы, львы, медведи, обезьяны и многие другие. Кроме того, в зоопарке есть контактный зоопарк «Русская деревня», где посетители могут покормить животных морковкой, купленной на входе.\n" +
                            "ARCHITECTURE\n" +
                            "56.335080, 43.853725\n" +
                            "\n" +
                            "Галерея кукол Хрупкие мечты\n" +
                            "Галерея кукол «Хрупкие мечты» — это уникальное место, где вы можете познакомиться с коллекцией кукол, созданных мастером Хильдегардом Гюнцелем. Посетители отмечают, что каждая кукла — это настоящее произведение искусства, созданное с большой любовью и вниманием к деталям. Во время экскурсии вы узнаете историю создания каждой куклы, а также увидите фильм о процессе их создания. Кроме того, в галерее есть сувенирный магазин, где можно приобрести игрушки и елочные украшения.\n" +
                            "ARCHITECTURE\n" +
                            "56.333123, 43.902013\n" +
                            "\n" +
                            "Церковь Смоленской иконы Божией Матери\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.328914, 43.936032\n" +
                            "\n" +
                            "Церковь Владимирской иконы Божией Матери\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.328721, 43.936453\n" +
                            "\n" +
                            "Собор Всемилостивого Спаса и Происхождения честных древ Животворящего Креста Господня Староярмарочный\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.331392, 43.953961\n" +
                            "\n" +
                            "Ярмарочные торговые ряды\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.331331, 43.959668\n" +
                            "\n" +
                            "Нижегородская ярмарка\n" +
                            "Выставочный центр «Нижегородская ярмарка» — это место, где можно не только приобрести сувениры, но и узнать много интересного об истории России. Внутри здания находится интерактивный мультимедийный музей, который знакомит посетителей с историей России от Рюрика до Романовых. Кроме того, на территории выставочного центра есть каток, лабиринт изо льда, сувенирные лавки, а также кафе и рестораны.\n" +
                            "ARCHITECTURE\n" +
                            "56.328299, 43.961199\n" +
                            "\n" +
                            "Метромост\n" +
                            "«Метромост» — это достопримечательность Нижнего Новгорода, которая соединяет две части города через реку Волгу. Он был построен в 1992 году и с тех пор стал неотъемлемой частью городской инфраструктуры. Мост имеет длину около 2,2 км и состоит из двух частей: автомобильной и железнодорожной. На мосту есть смотровые площадки, откуда открывается прекрасный вид на город и реку. В ночное время мост подсвечивается, что делает его еще более привлекательным для туристов.\n" +
                            "ARCHITECTURE\n" +
                            "56.319127, 43.964939\n" +
                            "\n" +
                            "Нижегородский государственный цирк имени Маргариты Назаровой\n" +
                            "В Нижегородском государственном цирке им. Маргариты Назаровой можно увидеть выступления акробатов, воздушных гимнастов, дрессировщиков, клоунов, а также шоу с участием белых медведей, собак породы самоед и других животных. В цирке есть два больших манежа, а также автономная котельная.\n" +
                            "ARCHITECTURE\n" +
                            "56.318547, 43.953340\n" +
                            "\n" +
                            "Церковь Преображения Господня\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.314680, 43.950377\n" +
                            "\n" +
                            "Канавинский мост\n" +
                            "Канавинский мост — это автомобильно-пешеходный мост через реку Оку, соединяющий верхнюю часть города с заречной. Он был построен в 1896 году и является одной из главных достопримечательностей Нижнего Новгорода. С него открывается прекрасный вид на Стрелку, Нижегородскую ярмарку, Кремль и другие достопримечательности города. В вечернее время мост красиво подсвечивается.\n" +
                            "ARCHITECTURE\n" +
                            "56.327901, 43.972852\n" +
                            "\n" +
                            "Городская усадьба\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.319482, 43.950879\n" +
                            "\n" +
                            "Церковь Преображения Господня\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.314680, 43.950377\n" +
                            "\n" +
                            "Дворец культуры имени В.И. Ленина\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.307442, 43.934780\n" +
                            "\n" +
                            "Вокзал станции детской железной дороги Родина\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.308124, 43.933696\n" +
                            "\n" +
                            "Церковь иконы Божией Матери Умиление\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.291542, 43.945094\n" +
                            "\n" +
                            "Мельница Якова Башкирова\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.313786, 43.952447\n" +
                            "\n" +
                            "Башня в честь 50-летия Победы\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.298839, 43.944449\n" +
                            "\n" +
                            "Паровозы России\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.284043, 43.845962\n" +
                            "\n" +
                            "Церковь Благая весть\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.301115, 43.859953\n" +
                            "\n" +
                            "Старообрядческая церковь\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.316337, 43.882035\n" +
                            "\n" +
                            "Церковь Лоза\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.293885, 43.852512\n" +
                            "\n" +
                            "Музей истории развития Горьковской железной дороги\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.287978, 43.852108\n" +
                            "\n" +
                            "Церковь Гавриила Архангела\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.232043, 43.717345\n" +
                            "\n" +
                            "Церковь Покрова Пресвятой Богородицы\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.265254, 43.753381\n" +
                            "\n" +
                            "Храм в честь иконы Божией Матери Неопалимая Купина\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.245681, 43.749569\n" +
                            "\n" +
                            "Церковь Илии Пророка\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.232049, 43.760967\n" +
                            "\n" +
                            "Храм в честь Собора Архистратига Михаила и прочих Небесных сил бесплотных\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.232738, 43.818740\n" +
                            "\n" +
                            "Церковь Пантелеимона Целителя\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.249215, 43.827702\n" +
                            "\n" +
                            "Церковь иконы Божией Матери Прибавление Ума\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.243723, 43.830920\n" +
                            "\n" +
                            "Храм в честь Собора Архистратига Михаила и прочих Небесных сил бесплотных\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.232738, 43.818740\n" +
                            "\n" +
                            "Водонапорная башня\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.217925, 43.798404\n" +
                            "\n" +
                            "Аэровокзал местных линий\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.215068, 43.800422\n" +
                            "\n" +
                            "Музей боевой и трудовой славы Горьковского объединенного авиаотряда\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.215203, 43.801412\n" +
                            "\n" +
                            "Бусыгинский дом\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.253834, 43.865384\n" +
                            "\n" +
                            "Жилой дом нач. 1930-х-ансамбль застройки жилого квартала № 3 Соцгорода\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.251300, 43.865400\n" +
                            "\n" +
                            "Монумент боевой и трудовой славы автозаводцев\n" +
                            "Монумент боевой и трудовой славы автозаводцев — это памятник, посвященный героям Великой Отечественной войны и труженикам тыла, которые работали на Горьковском автомобильном заводе во время войны. Памятник представляет собой двухуровневый постамент с бронзовой чашей, в которой горит Вечный огонь. Вокруг памятника разбит красивый парк, где можно погулять и отдохнуть.\n" +
                            "ARCHITECTURE\n" +
                            "56.246614, 43.872318\n" +
                            "\n" +
                            "Здание Автозаводского универмага\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.243024, 43.864702\n" +
                            "\n" +
                            "Церковь иконы Божией Матери Прибавление Ума\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.243723, 43.830920\n" +
                            "\n" +
                            "Радиусный дом\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.243518, 43.847611\n" +
                            "\n" +
                            "Церковь святой преподобной Марии Египетской\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.240044, 43.839710\n" +
                            "\n" +
                            "Церковь преподобной мученицы великой княгини Елисаветы Феодоровны\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.232956, 43.844201\n" +
                            "\n" +
                            "Киноконцертный зал Дворца культуры ГАЗ им. В. М. Молотова\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.241016, 43.859752\n" +
                            "\n" +
                            "Автозаводский парк\n" +
                            "Автозаводский парк культуры и отдыха — это одно из самых популярных мест для отдыха и развлечений в Нижнем Новгороде. Он расположен на территории Автозаводского района и предлагает своим посетителям множество аттракционов, кафе и ресторанов, а также каток, который работает круглый год. Кроме того, в парке есть детский городок, где дети могут играть и кататься на горках. Зимой парк превращается в настоящую зимнюю сказку с ледяными горками и лабиринтами.\n" +
                            "ARCHITECTURE\n" +
                            "56.239942, 43.859180\n" +
                            "\n" +
                            "Чиполлино\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.239282, 43.854818\n" +
                            "\n" +
                            "Церковь Христиан Адвентистов Седьмого Дня\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.222335, 43.849257\n" +
                            "\n" +
                            "Церковь благоверного князя Георгия Всеволодовича\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.220918, 43.868789\n" +
                            "\n" +
                            "Церковь Троицы Живоначальной при Архиерейском подворье\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.232877, 43.878918\n" +
                            "\n" +
                            "Вокзал станции детской железной дороги Счастливая\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.258628, 43.869782\n" +
                            "\n" +
                            "Церковь евангельских христиан-баптистов\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.259643, 43.883013\n" +
                            "\n" +
                            "Аллея автозаводских олимпийских звёзд\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.262131, 43.877705\n" +
                            "\n" +
                            "Церковь Татианы Великомученицы\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.261954, 43.878932\n" +
                            "\n" +
                            "Парк имени 777-летия Нижнего Новгорода\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.262640, 43.875303\n" +
                            "\n" +
                            "Мечеть Тауба\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.262860, 43.891284\n" +
                            "\n" +
                            "Музей истории ГАЗ\n" +
                            "Музей истории ГАЗ — это место, которое обязательно стоит посетить всем, кто интересуется автомобилями и историей. В музее представлено более 40 тысяч экспонатов, в том числе автомобили, архивы и интерактивные стенды, которые позволяют посетителям узнать больше о традициях завода и автомобильной культуре. Кроме того, музей входит в каталог автомобильных музеев Европы и участвует в различных выставках и форумах.\n" +
                            "ARCHITECTURE\n" +
                            "56.255355, 43.894809\n" +
                            "\n" +
                            "Музей Нижегородского метрополитена\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.252168, 43.888819\n" +
                            "\n" +
                            "Спасо-Преображенская церковь\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.256310, 43.934082\n" +
                            "\n" +
                            "Автозаводский слив\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.250110, 43.929470\n" +
                            "\n" +
                            "Православный приход церкви во имя святого пророка Илии\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.203578, 43.792098\n" +
                            "\n" +
                            "Церковь Рождества Богородицы\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.209320, 43.756440\n" +
                            "\n" +
                            "Часовня иконы Божией Матери Живоносный Источник на Барановом ключе\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.198799, 43.977474\n" +
                            "\n" +
                            "Церковь Пантелеимона целителя\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.229740, 43.943154\n" +
                            "\n" +
                            "Музей А.Д. Сахарова\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.232287, 43.950423\n" +
                            "\n" +
                            "Церковь Казанской иконы Божией Матери\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.233122, 44.000427\n" +
                            "\n" +
                            "Церковь Новомучеников и Исповедников Нижегородских\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.230262, 43.943488\n" +
                            "\n" +
                            "Храм в честь святого великомученика Георгия Победоносца\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.237409, 43.977678\n" +
                            "\n" +
                            "Церковь святой мученицы княгини Людмилы Чешской\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.257893, 44.074269\n" +
                            "\n" +
                            "Храм Рождества Пресвятой Богородицы\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.247990, 44.081570\n" +
                            "\n" +
                            "Храм в честь Иконы Божией Матери Утоли моя печали\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.229381, 44.077047\n" +
                            "\n" +
                            "Парк аттракционов\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.224584, 44.073821\n" +
                            "\n" +
                            "КидБург Эксперименты\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.225438, 44.070647\n" +
                            "\n" +
                            "Церковь Преображения Господня в Федяково\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.231229, 44.056883\n" +
                            "\n" +
                            "Храм во имя святой Матроны Московской\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.258436, 44.012673\n" +
                            "\n" +
                            "Оранжерея Ботанического Сада ННГУ\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.249721, 44.002784\n" +
                            "\n" +
                            "Церковь иконы Божией Матери Взыскание погибших\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.241851, 43.973463\n" +
                            "\n" +
                            "Жилые дома работников телефонного завода товарищества Сименс и Гальске\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.249457, 43.965678\n" +
                            "\n" +
                            "Музей истории Приокского района\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.260141, 43.974110\n" +
                            "\n" +
                            "Завод Сименс и Гальске\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.252584, 43.967790\n" +
                            "\n" +
                            "Тобольские казармы\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.285730, 43.981586\n" +
                            "\n" +
                            "Смотровая площадка\n" +
                            "Эта смотровая площадка — место, откуда открывается великолепный вид на правый берег реки Оки. Посетители отмечают, что особенно красиво здесь во время полноводья, когда Оку можно увидеть во всей ее красе. Кроме того, с площадки открывается вид на памятник природы — урочище «Слуда», который простирается от моста Молитовский до моста Карповский.\n" +
                            "ARCHITECTURE\n" +
                            "56.278025, 43.973423\n" +
                            "\n" +
                            "Парк Швейцария\n" +
                            "Парк «Швейцария» является одним из самых популярных мест для отдыха в Нижнем Новгороде. Он был основан более 80 лет назад и за это время претерпел множество изменений. После масштабной реконструкции парк стал еще более привлекательным для посетителей. Здесь есть множество аттракционов, зоопарк, веревочный городок, планетарий и другие развлечения. Кроме того, парк славится своей живописной природой и красивыми видами на реку Оку.\n" +
                            "ARCHITECTURE\n" +
                            "56.274490, 43.973357\n" +
                            "\n" +
                            "Покровская церковь\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.275334, 44.012369\n" +
                            "\n" +
                            "Архитектурно-этнографический музей-заповедник Щёлоковский хутор\n" +
                            "Щёлоковский хутор — это этнографический парк-музей под открытым небом, расположенный в черте Нижнего Новгорода. Он был основан в 1969 году на месте бывшего хутора Махотина, который был выкуплен купцом Щелоковым в середине 1800-х годов. В музее можно увидеть крестьянские избы, амбары и церкви, а также водяную мельницу, которая работает до сих пор. На территории музея проводятся интерактивные программы и хороводы.\n" +
                            "ARCHITECTURE\n" +
                            "56.274018, 44.010644\n" +
                            "\n" +
                            "Церковь Владимирской-Оранской Богоматери и защитников Отечества\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.294446, 44.029746\n" +
                            "\n" +
                            "Церковь Игоря Черниговского\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.289081, 44.065876\n" +
                            "\n" +
                            "Приход Святой равноапостольной княгини Ольги\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.288945, 44.065051\n" +
                            "\n" +
                            "Парк чудес Галилео\n" +
                            "Музей «Парк чудес Галилео» — это место, где вы можете узнать много нового и интересного о физике, химии и других науках. В музее есть множество интерактивных экспонатов, которые помогут вам понять, как работают различные явления. Кроме того, в музее проводятся мастер-классы и шоу с удивительными экспериментами.\n" +
                            "ARCHITECTURE\n" +
                            "56.290522, 44.072432\n" +
                            "\n" +
                            "Церковь Преображения Господня\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.318922, 44.072041\n" +
                            "\n" +
                            "Церковь Успения Пресвятой Богородицы\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.323099, 44.050443\n" +
                            "\n" +
                            "Вознесенский Печерский мужской монастырь\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.323072, 44.049733\n" +
                            "\n" +
                            "Церковь Живоначальной Троицы\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.308588, 44.046822\n" +
                            "\n" +
                            "Дом, где жил Борис Немцов\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.300783, 44.026681\n" +
                            "\n" +
                            "Музей техники и оборонной промышленности\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.328114, 44.035755\n" +
                            "\n" +
                            "Увидеть и полюбить\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.324540, 44.035847\n" +
                            "\n" +
                            "Особняк О.И. Каменской, 1912 - 1913 годы\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.327374, 44.024313\n" +
                            "\n" +
                            "Крестьянский поземельный банк\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.325366, 44.012422\n" +
                            "\n" +
                            "Нижегородская уездная земская управа, арестный дом\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.325246, 44.024203\n" +
                            "\n" +
                            "Дом М. М. Рукавишникова\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.325326, 44.021870\n" +
                            "\n" +
                            "Усадьба В.М. Рукавишникова\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.325191, 44.019477\n" +
                            "\n" +
                            "Музей-квартира А.М. Горького\n" +
                            "Музей-квартира А.М. Горького — это филиал государственного музея, расположенного в Нижнем Новгороде на улице Семашко. Здесь можно узнать много интересного о жизни и творчестве великого русского писателя. В музее представлены его личные вещи, документы и награды, а также экспонаты, связанные с друзьями и коллегами писателя. Посетители отмечают, что в музее сохранилась атмосфера того времени, когда здесь жил Горький.\n" +
                            "ARCHITECTURE\n" +
                            "56.322778, 44.018558\n" +
                            "\n" +
                            "Приют для подкидышей\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.322642, 44.019163\n" +
                            "\n" +
                            "Дом трудолюбия\n" +
                            "Дом трудолюбия — это историческое здание в центре Нижнего Новгорода, которое было построено в конце XIX века и первоначально использовалось как типография. В настоящее время в здании расположены различные организации, кружки и секции, а также проводятся различные мероприятия.\n" +
                            "ARCHITECTURE\n" +
                            "56.321247, 44.012840\n" +
                            "\n" +
                            "Церковь Всемилостивейшего Спаса\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.319070, 44.024443\n" +
                            "\n" +
                            "Нижегородский острог\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.317182, 44.017035\n" +
                            "\n" +
                            "Усадьба Рукавишниковых\n" +
                            "Музей-усадьба «Рукавишниковы» — это место, где можно погрузиться в атмосферу купеческого быта и узнать много интересного об истории города и его знаменитых жителях, таких как летчики Петр Нестеров и Валерий Чкалов. Посетители отмечают, что интерьеры музея поражают своей пышностью и богатством, особенно парадная лестница и бальный зал, которые украшены фресками, лепниной и зеркалами. Также в музее можно увидеть коллекцию фарфора, мебели и других предметов интерьера.\n" +
                            "ARCHITECTURE\n" +
                            "56.329333, 44.016239\n" +
                            "\n" +
                            "Чкаловская лестница\n" +
                            "Чкаловская лестница является одним из символов Нижнего Новгорода и представляет собой монументальную лестницу, соединяющую набережные Верхней Волги и Нижней Волги. Она была спроектирована архитекторами Александром Яковлевым, Львом Рудневым и Владимиром Мунцем и названа в честь летчика Валерия Чкалова. Лестница была построена в честь Сталинградской битвы и открыта в 1949 году, а ее название было изменено на Чкаловскую лестницу.\n" +
                            "ARCHITECTURE\n" +
                            "56.330872, 44.009461\n" +
                            "\n" +
                            "Нижегородский государственный художественный музей\n" +
                            "Художественный музей в Нижнем Новгороде является старейшим музеем России и одним из крупнейших музеев изобразительного искусства. Коллекция музея состоит из более чем 12 000 произведений отечественных и зарубежных художников, в том числе национальных и мировых шедевров. Среди них можно увидеть картины русских художников XVIII-XIX веков, работы авангардистов и современных художников, а также западноевропейское искусство XV-начала XX века, представленное работами итальянских, немецких, фламандских, голландских, французских и английских художников.\n" +
                            "ARCHITECTURE\n" +
                            "56.329491, 44.006371\n" +
                            "\n" +
                            "Тоннель Нижегородского фуникулёра\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.329037, 43.998282\n" +
                            "\n" +
                            "Нижегородский кремль\n" +
                            "Нижегородский кремль — это одна из главных достопримечательностей Нижнего Новгорода. Он был основан в XII веке и служил оборонительной крепостью. В XVI-XVII веках кремль выполнял административные функции, а в XX веке был реконструирован. В настоящее время на территории кремля находится множество музеев, храмов, памятников и других зданий. Также здесь есть смотровые площадки, откуда открывается прекрасный вид на реку Волгу.\n" +
                            "ARCHITECTURE\n" +
                            "56.328437, 44.003111\n" +
                            "\n" +
                            "Дмитриевская башня\n" +
                            "Дмитриевская башня, также известная как Дмитровская башня, является проездной башней Нижегородского Кремля и считается главным входом в крепость. Она была построена в 1500 году и с тех пор несколько раз перестраивалась. В настоящее время башня выполнена в русском стиле и украшена иконами. Внутри башни находится информационный пункт, где можно получить информацию о крепости и ее достопримечательностях.\n" +
                            "ARCHITECTURE\n" +
                            "56.327041, 44.005704\n" +
                            "\n" +
                            "Площадь Минина и Пожарского\n" +
                            "На центральной площади Нижнего Новгорода, названной в честь Кузьмы Минина и князя Дмитрия Пожарского, расположены такие достопримечательности, как Дмитриевская башня, Пороховая и Георгиевская башни Нижегородского кремля, Дворец Труда, а также памятник Минину и памятник летчику Валерию Чкалову. Кроме того, на площади находится первый городской фонтан, который красиво подсвечивается по вечерам.\n" +
                            "ARCHITECTURE\n" +
                            "56.326850, 44.007177\n" +
                            "\n" +
                            "Нижегородский экзотариум\n" +
                            "«Нижегородский экзотариум» — это зоопарк, расположенный в историческом центре Нижнего Новгорода, на улице Большая Покровская, дом 18, на пешеходной туристической тропе. В этом зоопарке можно увидеть множество интересных животных, таких как змеи, ящерицы, лягушки, пауки, насекомые, птицы, совы, филины, питоны, черепахи и многие другие.\n" +
                            "ARCHITECTURE\n" +
                            "56.322117, 44.001142\n" +
                            "\n" +
                            "Смайл Парк\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.321441, 43.999330\n" +
                            "\n" +
                            "Государственный банк, 1911 - 1913 годы\n" +
                            "Здание Государственного банка, 1911 — 1913 годы было построено в неорусском стиле по проекту Владимира Покровского и отделано серым финским мрамором. Оно украшено гербами Российской империи и СССР, а также российским флагом. Внутри здания можно увидеть красивые росписи, созданные Иваном Билибиным. Этот памятник архитектуры является одним из символов Нижнего Новгорода и привлекает туристов со всего мира.\n" +
                            "ARCHITECTURE\n" +
                            "56.320036, 43.998805\n" +
                            "\n" +
                            "Стеклянный лабиринт\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.319224, 43.997585\n" +
                            "\n" +
                            "Технический музей\n" +
                            "Технический музей в Нижнем Новгороде — это место, где можно узнать о различных механизмах и приборах, которые использовались в прошлом. Здесь можно увидеть старинные инструменты, станки, велосипеды и многое другое. Кроме того, в музее есть уголок, посвященный СССР, где представлены предметы, связанные с этой эпохой.\n" +
                            "ARCHITECTURE\n" +
                            "56.318164, 43.995279\n" +
                            "\n" +
                            "Церковь Успения Пресвятой Девы Марии\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.317853, 44.000931\n" +
                            "\n" +
                            "Дом, где жил Максим Горький\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.313251, 43.988423\n" +
                            "\n" +
                            "Камень желаний\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.308880, 43.998752\n" +
                            "\n" +
                            "Комплекс зданий Городской тюрьмы\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.298164, 43.986906\n" +
                            "\n" +
                            "Водозаборная башня Куйбышевской водонапорной станции\n" +
                            "\n" +
                            "ARCHITECTURE\n" +
                            "56.305406, 43.967948\n" +
                            "\n" +
                            "Стрелка\n" +
                            "«Стрелка» — это одно из самых живописных мест в Нижнем Новгороде, где сливаются две реки — Волга и Ока. Здесь можно прогуляться по набережной, наслаждаясь прекрасным видом на исторический центр города и его окрестности.  На территории «Стрелки» есть несколько достопримечательностей, таких как храм Александра Невского, ажурные конструкции, созданные инженером Шуховым, и стадион, построенный к Чемпионату мира по футболу в 2018 году.\n" +
                            "ARCHITECTURE\n" +
                            "56.334548, 43.976030\n"
                    interestPoints.forEach {interestPoint ->
                        val ref = dataBase!!.pointReference.push()
                        interestPoint.data.id=ref.key.toString()
                        ref.setValue(interestPoint)
                    }
                }
            }
        }
        fun closeMenus(){
            mainActivity!!.binding.navView.selectedItemId = R.id.navigation_map
        }
    }
}