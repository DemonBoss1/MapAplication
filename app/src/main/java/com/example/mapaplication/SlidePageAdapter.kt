package com.example.mapaplication

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.mapaplication.MainActivity
import java.util.ArrayList

class SlidePageAdapter(activity: MainActivity?, val fragmentList: ArrayList<Fragment>) : FragmentStateAdapter(activity!!) {
    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }

    override fun getItemCount(): Int {
        return fragmentList.size
    }
}