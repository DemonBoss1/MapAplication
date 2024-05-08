package com.example.mapaplication

import android.content.Context
import android.content.SharedPreferences

class Setting private constructor(pref: SharedPreferences) {
    companion object{
        lateinit var pref: SharedPreferences
        lateinit var username: String

        fun get(prefs: SharedPreferences){
            pref = prefs
        }
    }
}