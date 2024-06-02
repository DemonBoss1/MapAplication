package com.example.mapaplication

import android.content.SharedPreferences
import com.example.mapaplication.ui.map.HistoryItem
import com.example.mapaplication.ui.map.InterestPoint

class SaveData private constructor(pref: SharedPreferences) {
    companion object{
        const val HistoryKeys = "history points"

        lateinit var pref: SharedPreferences
        lateinit var username: String
        lateinit var UserId: String
        var currentPointId: String? = null
        var historyPoints = arrayListOf<HistoryItem>()

        fun get(prefs: SharedPreferences){
            pref = prefs
        }
    }
}