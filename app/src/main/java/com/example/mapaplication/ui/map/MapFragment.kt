package com.example.mapaplication.ui.map

import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mapaplication.DataBase
import com.example.mapaplication.Filters
import com.example.mapaplication.MapManager
import com.example.mapaplication.Message
import com.example.mapaplication.Setting
import com.example.mapaplication.User
import com.example.mapaplication.databinding.FragmentMapBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.values
import com.google.firebase.database.values
import com.squareup.picasso.Picasso
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.mapview.MapView
import kotlinx.coroutines.flow.Flow
import java.util.Date

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
                messageSet.setText("")
            }
            sentMessage.setOnClickListener {
                val message = messageSet.text.toString()
                messageGet.text = message
                messageSet.text.clear()

                DataBase.getDataBase()!!
                    .userReference
                    .child(Setting.ID)
                    .get().addOnCompleteListener {
                        val user: User? = it.result.getValue<User>()
                        if (user != null)
                            Picasso.get().load(user.imageUri).into(userImageInMessage)
                    }

                username.text = Setting.username

                val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
                val currentDate = sdf.format(Date())
                dateTime.text = currentDate

                DataBase.getDataBase()!!.messageReference.push()
                    .setValue(
                        Message(message, Setting.ID, currentDate)
                    )
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