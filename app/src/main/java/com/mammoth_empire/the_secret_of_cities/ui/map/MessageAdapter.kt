package com.mammoth_empire.the_secret_of_cities.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mammoth_empire.the_secret_of_cities.DataBase
import com.example.mapaplication.R
import com.mammoth_empire.the_secret_of_cities.User
import com.example.mapaplication.databinding.MessageLayoutBinding
import com.google.firebase.database.getValue
import com.squareup.picasso.Picasso

class MessageAdapter(private val messageList: ArrayList<Message>): RecyclerView.Adapter<MessageAdapter.MessageHolder>() {
    class MessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = MessageLayoutBinding.bind(itemView)
        fun bind(message: Message) = with(binding) {
            var user: User?
            DataBase.getDataBase()!!
                .userReference
                .child(message.userId)
                .get().addOnCompleteListener {
                    user= it.result.getValue<User>()
                    if (user != null) {
                        Picasso.get().load(user!!.imageUri).into(userImageInMessage)
                        username.text = user!!.username
                    }
                }
            dateTime.text = message.date
            messageGet.text = message.message
            message.reviewList.forEachIndexed { index, b ->
                if(b) imageViewLayout.getChildAt(index).visibility = View.VISIBLE
                else imageViewLayout.getChildAt(index).visibility = View.GONE
            }
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