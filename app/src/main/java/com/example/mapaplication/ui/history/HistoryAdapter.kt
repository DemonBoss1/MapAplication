package com.example.mapaplication.ui.history

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mapaplication.DataBase
import com.example.mapaplication.Setting
import com.example.mapaplication.User
import com.example.mapaplication.databinding.MessageLayoutBinding
import com.example.mapaplication.ui.map.InterestPoint
import com.example.mapaplication.ui.map.Message
import com.example.mapaplication.ui.map.MessageAdapter
import com.google.firebase.database.getValue
import com.squareup.picasso.Picasso

class HistoryAdapter(historyList: ArrayList<InterestPoint>): RecyclerView.Adapter<HistoryAdapter.HistoryHolder>()  {
    class HistoryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = MessageLayoutBinding.bind(itemView)
        fun bind(message: Message) = with(binding){

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
        TODO("Not yet implemented")
    }

}
