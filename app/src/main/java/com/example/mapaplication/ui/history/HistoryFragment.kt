package com.example.mapaplication.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapaplication.databinding.FragmentHistoryBinding
import com.example.mapaplication.ui.map.InterestPoint
import com.example.mapaplication.ui.map.Message
import com.example.mapaplication.ui.map.MessageAdapter

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    val historyList = ArrayList<InterestPoint>()
    val adapter = HistoryAdapter(historyList)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.historyList.layoutManager = LinearLayoutManager(activity)
        binding.historyList.adapter = adapter

        return root
    }

    override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
    }
}