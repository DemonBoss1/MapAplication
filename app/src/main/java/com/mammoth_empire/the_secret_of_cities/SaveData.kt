package com.mammoth_empire.the_secret_of_cities

import android.content.SharedPreferences
import com.mammoth_empire.the_secret_of_cities.ui.map.HistoryItem

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