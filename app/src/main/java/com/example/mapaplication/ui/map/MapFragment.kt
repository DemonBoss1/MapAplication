package com.example.mapaplication.ui.map

import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mapaplication.DataBase
import com.example.mapaplication.Filters
import com.example.mapaplication.InterestPoint
import com.example.mapaplication.MapManager
import com.example.mapaplication.PlacemarkType
import com.example.mapaplication.PlacemarkUserData
import com.example.mapaplication.R
import com.example.mapaplication.databinding.FragmentMapBinding
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
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

class MapFragment : Fragment() {
    private lateinit var mapView: MapView
    private var _binding: FragmentMapBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
                menuPoint.visibility = View.INVISIBLE
            }
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

                MapManager.resetFilter()
            }
            filterApply.setOnClickListener {

                val filter = Filters(
                    checkBoxCafe.isChecked,
                    checkBoxHotel.isChecked,
                    checkBoxLandmark.isChecked
                )
                MapManager.setFilters(filter)

                MapManager.filterApply()

                filtersButton.visibility = View.VISIBLE
                filterMenu.visibility = View.INVISIBLE

            }
        }

        MapManager.get(this)!!.init()
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