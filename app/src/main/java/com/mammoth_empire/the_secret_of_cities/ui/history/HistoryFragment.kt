package com.mammoth_empire.the_secret_of_cities.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapaplication.databinding.FragmentHistoryBinding
import com.mammoth_empire.the_secret_of_cities.ui.map.InterestPoint

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    val historyList = ArrayList<InterestPoint>()
    val adapter = HistoryAdapter(historyList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        historyFragment = this
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.historyList.layoutManager = LinearLayoutManager(activity)
        binding.historyList.adapter = adapter

        //adapter.notifyDataSetChanged()

        return root
    }

    override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
    }

    companion object {
        private var historyFragment: HistoryFragment? = null

        fun notifyDataSetChanged(){
            historyFragment?.adapter?.notifyDataSetChanged()
        }
    }
}