package com.ruege.mobile.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.ruege.mobile.databinding.FragmentStatisticsByTypeBinding
import com.ruege.mobile.model.EssayResultData
import com.ruege.mobile.ui.adapter.StatisticsAdapter
import com.ruege.mobile.ui.viewmodel.PracticeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StatisticsByEssayFragment : Fragment() {

    private val viewModel: PracticeViewModel by activityViewModels()
    private lateinit var adapter: StatisticsAdapter

    private var _binding: FragmentStatisticsByTypeBinding? = null
    private val binding get() = _binding!!

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

        binding.tvEmptyStatistics.text = "Здесь будет отображаться статистика по сочинениям."

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = StatisticsAdapter(
            onPracticeClick = { 
                Toast.makeText(requireContext(), "Для сочинений нет отдельной практики", Toast.LENGTH_SHORT).show()
            },
            onStatisticsClick = { egeNumber ->
                showEssayResultDialog(egeNumber)
            }
        )
        binding.recyclerStatistics.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.statisticsByEssay.observe(viewLifecycleOwner) { statistics ->
            if (statistics.isNullOrEmpty()) {
                binding.recyclerStatistics.visibility = View.GONE
                binding.tvEmptyStatistics.visibility = View.VISIBLE
            } else {
                binding.recyclerStatistics.visibility = View.VISIBLE
                binding.tvEmptyStatistics.visibility = View.GONE
                adapter.submitList(statistics)
            }
        }
    }

    private fun showEssayResultDialog(egeNumber: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val stats = viewModel.getStatisticsByEgeNumber(egeNumber)
            val essayData = stats?.variantData?.let { EssayResultData.fromJsonString(it) }

            if (essayData != null) {
                val dialogBuilder = AlertDialog.Builder(requireContext())
                dialogBuilder.setTitle(egeNumber.removePrefix("essay:"))
                
                val message = "Результат:\n${essayData.result}\n\nВаше сочинение:\n${essayData.essayContent}"
                dialogBuilder.setMessage(message)
                
                dialogBuilder.setPositiveButton("Закрыть") { dialog, _ ->
                    dialog.dismiss()
                }
                
                dialogBuilder.show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Нет данных о результате сочинения",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 