package com.example.mapaplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mapaplication.databinding.ActivityMainBinding
import com.example.mapaplication.ui.history.HistoryFragment
import com.example.mapaplication.ui.map.MapFragment
import com.yandex.mapkit.MapKitFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val fragmentList = arrayListOf(MapFragment.newInstance(), HistoryFragment())
    var active = fragmentList[0]

    private val getImageForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK){
            binding.userImage.setImageURI(result.data?.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey("8e68e3bf-421b-4ae6-ac02-7d71e41c9c36")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recordingUserName()

        binding.userImage.setOnClickListener {
            intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            getImageForResult.launch(intent)
        }

        binding.navHostFragmentActivityMain.isUserInputEnabled = false
        val pagerAdapter = SlidePageAdapter(this, fragmentList)
        binding.navHostFragmentActivityMain.setAdapter(pagerAdapter)

//        supportFragmentManager.beginTransaction().add(R.id.nav_host_fragment_activity_main, fragmentList[1], "2").commit()
//        supportFragmentManager.beginTransaction().add(R.id.nav_host_fragment_activity_main, fragmentList[0], "1").commit()

        binding.navView.setOnItemSelectedListener {
            when(it.itemId){
                R.id.navigation_map -> {
                    binding.navHostFragmentActivityMain.currentItem = 0
                }
                R.id.navigation_history -> {
                    binding.navHostFragmentActivityMain.currentItem = 1
                }
            }
            true
        }
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

    override fun onDestroy() {
        super.onDestroy()
    }
}