package com.mammoth_empire.the_secret_of_cities.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mammoth_empire.the_secret_of_cities.MainActivity
import com.example.mapaplication.R
import com.mammoth_empire.the_secret_of_cities.SaveData
import com.example.mapaplication.databinding.HistoryItemBinding
import com.mammoth_empire.the_secret_of_cities.ui.map.HistoryItem
import com.mammoth_empire.the_secret_of_cities.ui.map.InterestPoint
import com.mammoth_empire.the_secret_of_cities.ui.map.MapManager
import com.yandex.mapkit.geometry.Point

class HistoryAdapter(historyList: ArrayList<InterestPoint>): RecyclerView.Adapter<HistoryAdapter.HistoryHolder>()  {
    private val historyList = SaveData.historyPoints
    class HistoryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = HistoryItemBinding.bind(itemView)
        fun bind(item: HistoryItem) = with(binding){
            historyName.text = item.nameHistoryPoint
            dateTimeHistory.text = item.date
            movingToPointInterest.setOnClickListener {
                MapManager.movePosition(Point(item.point.latitude, item.point.longitude), 17.0f)
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
