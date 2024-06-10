package com.example.mapaplication.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mapaplication.DataBase
import com.example.mapaplication.R
import com.example.mapaplication.User
import com.example.mapaplication.databinding.ReviewItemBinding
import com.google.firebase.database.getValue
import com.squareup.picasso.Picasso

class ReviewAdapter(private val reviewList: ArrayList<ReviewItem>): RecyclerView.Adapter<ReviewAdapter.ReviewHolder>() {
    class ReviewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val binding = ReviewItemBinding.bind(itemView)

        fun bind(reviewItem: ReviewItem) = with(binding) {
            imageReview.setImageResource(reviewItem.imageId)
            countImage.text = reviewItem.count.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.review_item, parent, false)
        return ReviewHolder(view)
    }

    override fun getItemCount(): Int {
        return reviewList.size
    }

    override fun onBindViewHolder(holder: ReviewHolder, position: Int) {
        holder.bind(reviewList[position])
    }
}