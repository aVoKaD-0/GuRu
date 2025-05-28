package com.ruege.mobile.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.ruege.mobile.R
import com.ruege.mobile.ui.adapter.StatisticsAdapter
import com.ruege.mobile.viewmodel.PracticeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StatisticsByTypeFragment : Fragment() {

    private val viewModel: PracticeViewModel by activityViewModels()
    private lateinit var adapter: StatisticsAdapter
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistics_by_type, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.recyclerStatistics)
        emptyView = view.findViewById(R.id.tvEmptyStatistics)
        
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = StatisticsAdapter { egeNumber ->
            navigateToPractice(egeNumber)
        }
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.statisticsByType.observe(viewLifecycleOwner) { statistics ->
            if (statistics.isNullOrEmpty()) {
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
                adapter.submitList(statistics)
            }
        }
    }

    private fun navigateToPractice(egeNumber: String) {
        // Навигация к экрану практики по выбранному номеру ЕГЭ
        findNavController().navigate(
            R.id.navigation_exercises,
            Bundle().apply {
                putString("ege_number", egeNumber)
            }
        )
        
        // Показываем сообщение
        Toast.makeText(
            requireContext(),
            "Практика задания $egeNumber",
            Toast.LENGTH_SHORT
        ).show()
    }
} 