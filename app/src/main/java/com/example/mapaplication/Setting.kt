package com.example.mapaplication

import android.content.SharedPreferences

class Setting private constructor(pref: SharedPreferences) {
    companion object{
        lateinit var pref: SharedPreferences
        lateinit var username: String
        lateinit var UserId: String
        var currentPointId: String? = null

        fun get(prefs: SharedPreferences){
            pref = prefs
        }
    }
}