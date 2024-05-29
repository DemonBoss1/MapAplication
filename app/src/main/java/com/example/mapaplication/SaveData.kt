package com.example.mapaplication

import android.content.SharedPreferences
import com.example.mapaplication.ui.map.InterestPoint

class SaveData private constructor(pref: SharedPreferences) {
    companion object{
        lateinit var pref: SharedPreferences
        lateinit var username: String
        lateinit var UserId: String
        var currentPointId: String? = null
        val historyPoints = arrayListOf<InterestPoint>()

        fun get(prefs: SharedPreferences){
            pref = prefs
        }
    }
}