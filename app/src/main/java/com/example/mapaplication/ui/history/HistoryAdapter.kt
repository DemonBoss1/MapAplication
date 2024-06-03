package com.example.mapaplication.ui.history

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mapaplication.DataBase
import com.example.mapaplication.MainActivity
import com.example.mapaplication.R
import com.example.mapaplication.SaveData
import com.example.mapaplication.User
import com.example.mapaplication.databinding.HistoryItemBinding
import com.example.mapaplication.databinding.MessageLayoutBinding
import com.example.mapaplication.ui.map.HistoryItem
import com.example.mapaplication.ui.map.InterestPoint
import com.example.mapaplication.ui.map.MapManager
import com.example.mapaplication.ui.map.Message
import com.google.firebase.database.getValue
import com.squareup.picasso.Picasso
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition

class HistoryAdapter(historyList: ArrayList<InterestPoint>): RecyclerView.Adapter<HistoryAdapter.HistoryHolder>()  {
    private val historyList = SaveData.historyPoints
    class HistoryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = HistoryItemBinding.bind(itemView)
        fun bind(item: HistoryItem) = with(binding){
            historyName.text = item.nameHistoryPoint
            dateTimeHistory.text = item.date
            movingToPointInterest.setOnClickListener {
                MapManager.movePosition(CameraPosition(Point(item.point.latitude, item.point.longitude), 17.0f, 0f, 0f))
                MainActivity.closeMenus()
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        return HistoryHolder(view)
    }

    override fun getItemCount(): Int {
        return historyList.size
    }

    override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
        holder.bind(historyList[position])
    }

}
