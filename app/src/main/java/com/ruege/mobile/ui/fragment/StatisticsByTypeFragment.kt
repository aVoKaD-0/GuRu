package com.ruege.mobile.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.ruege.mobile.R
import com.ruege.mobile.databinding.FragmentStatisticsByTypeBinding
import com.ruege.mobile.model.VariantResult
import com.ruege.mobile.model.EssayResultData
import com.ruege.mobile.ui.adapter.StatisticsAdapter
import com.ruege.mobile.ui.viewmodel.PracticeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StatisticsByTypeFragment : Fragment() {

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
        
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = StatisticsAdapter(
            onPracticeClick = { egeNumber ->
                navigateToPractice(egeNumber)
            },
            onStatisticsClick = { egeNumber ->
                showVariantResultsDialog(egeNumber)
            }
        )
        binding.recyclerStatistics.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.statisticsByType.observe(viewLifecycleOwner) { statistics ->
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

    private fun navigateToPractice(egeNumber: String) {
        findNavController().navigate(
            R.id.navigation_exercises,
            Bundle().apply {
                putString("ege_number", egeNumber)
            }
        )
        
        Toast.makeText(
            requireContext(),
            "Практика задания $egeNumber",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun showVariantResultsDialog(egeNumber: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val variantResult = viewModel.getVariantResultForEgeNumber(egeNumber)
            
            if (variantResult != null) {
                val dialogBuilder = AlertDialog.Builder(requireContext())
                dialogBuilder.setTitle("Результаты варианта для задания $egeNumber")
                
                val message = buildVariantResultMessage(variantResult)
                dialogBuilder.setMessage(message)
                
                dialogBuilder.setPositiveButton("Закрыть") { dialog, _ ->
                    dialog.dismiss()
                }
                
                dialogBuilder.show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Нет данных о результатах варианта для задания $egeNumber",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun buildVariantResultMessage(variantResult: VariantResult): String {
        val sb = StringBuilder()
        
        sb.appendLine("Результат: ${variantResult.score} из ${variantResult.maxScore} баллов")
        sb.appendLine("Успешность: ${(variantResult.score * 100 / variantResult.maxScore.coerceAtLeast(1))}%")
        
        if (variantResult.tasks.isNotEmpty()) {
            sb.appendLine("\nДетальная информация:")
            
            variantResult.tasks.forEachIndexed { index, taskAnswer ->
                sb.appendLine("\nЗадание ${index + 1}:")
                sb.appendLine("Ответ: ${taskAnswer.userAnswer}")
                sb.appendLine("Верный ответ: ${taskAnswer.correctAnswer}")
                sb.appendLine("Статус: ${if (taskAnswer.isCorrect) "верно" else "неверно"}")
            }
        }
        
        return sb.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 