package com.example.mapaplication.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mapaplication.DataBase
import com.example.mapaplication.R
import com.example.mapaplication.SaveData
import com.example.mapaplication.User
import com.example.mapaplication.databinding.MessageLayoutBinding
import com.google.firebase.database.getValue
import com.squareup.picasso.Picasso

class MessageAdapter(private val messageList: ArrayList<Message>): RecyclerView.Adapter<MessageAdapter.MessageHolder>() {
    class MessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = MessageLayoutBinding.bind(itemView)
        fun bind(message: Message) = with(binding){
                DataBase.getDataBase()!!
                    .userReference
                    .child(SaveData.UserId)
                    .get().addOnCompleteListener {
                        val user: User? = it.result.getValue<User>()
                        if (user != null)
                            Picasso.get().load(user.imageUri).into(userImageInMessage)
                    }
                username.text = SaveData.username
                dateTime.text = message.date
                messageGet.text = message.message
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.message_layout, parent, false)
        return MessageHolder(view)
    }

    override fun onBindViewHolder(holder: MessageHolder, position: Int) {
        holder.bind(messageList[position])
    }

    override fun getItemCount(): Int {
        return messageList.size
    }
}