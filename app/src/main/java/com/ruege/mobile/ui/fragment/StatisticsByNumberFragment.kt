package com.ruege.mobile.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ruege.mobile.databinding.FragmentStatisticsByTypeBinding // Предполагаем такой layout
import com.ruege.mobile.ui.adapter.PracticeStatisticsAdapter
import com.ruege.mobile.ui.viewmodel.PracticeStatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StatisticsByNumberFragment : Fragment() {

    private var _binding: FragmentStatisticsByTypeBinding? = null
    private val binding get() = _binding!!

    private val practiceStatisticsViewModel: PracticeStatisticsViewModel by activityViewModels()
    private lateinit var statisticsAdapter: PracticeStatisticsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsByTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeTaskStatistics()
    }

    private fun setupRecyclerView() {
        statisticsAdapter = PracticeStatisticsAdapter()
        binding.recyclerStatistics.apply {
            adapter = statisticsAdapter
            layoutManager = LinearLayoutManager(context)
            // TODO: Добавить разделители, если нужно
        }
    }

    private fun observeTaskStatistics() {
        // Используем viewLifecycleOwner.lifecycleScope.launchWhenStarted
        // чтобы гарантировать, что код выполнится когда view готово и lifecycle как минимум STARTED
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            practiceStatisticsViewModel.allTaskStatistics.observe(viewLifecycleOwner) { statsList ->
                if (statsList != null && statsList.isNotEmpty()) {
                    android.util.Log.d("StatsByNumberFrag", "Observed data, submitting to adapter. Count: ${statsList.size}")
                    statisticsAdapter.submitList(statsList.toList()) // toList() для создания новой копии
                    binding.tvEmptyStatistics.visibility = View.GONE
                    binding.recyclerStatistics.visibility = View.VISIBLE
                } else {
                    android.util.Log.d("StatsByNumberFrag", "Observed data is null or empty.")
                    binding.tvEmptyStatistics.visibility = View.VISIBLE
                    binding.recyclerStatistics.visibility = View.GONE
                }
            }
            // Дополнительно, если LiveData уже имеет значение при старте корутины
            practiceStatisticsViewModel.allTaskStatistics.value?.let { currentStats ->
                if (statisticsAdapter.currentList.isEmpty() && currentStats.isNotEmpty()) {
                    android.util.Log.d("StatsByNumberFrag", "Initial data (on launchWhenStarted) found in LiveData, submitting to adapter. Count: ${currentStats.size}")
                    statisticsAdapter.submitList(currentStats.toList())
                    binding.tvEmptyStatistics.visibility = View.GONE
                    binding.recyclerStatistics.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 