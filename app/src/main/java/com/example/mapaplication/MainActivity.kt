package com.example.mapaplication

import android.content.Context
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.mapaplication.databinding.ActivityMainBinding
import com.yandex.mapkit.MapKitFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey("8e68e3bf-421b-4ae6-ac02-7d71e41c9c36")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recordingUserName()

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_map,
                R.id.navigation_history,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    fun recordingUserName(){
        Setting.get(getSharedPreferences("setting", Context.MODE_PRIVATE))
        if(Setting.pref.contains("username")){
            Setting.username = Setting.pref.getString("username", "").toString()
            binding.usernameMenu.visibility = View.INVISIBLE
        }
    }
    fun editName(){
        val username = binding.editTextName.text.toString()
        Setting.username = username
        val edit = Setting.pref.edit()
        edit.putString("username",username).apply()
        binding.usernameMenu.visibility = View.INVISIBLE

    }
}