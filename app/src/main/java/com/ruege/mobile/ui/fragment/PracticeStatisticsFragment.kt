package com.ruege.mobile.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ruege.mobile.R
import com.ruege.mobile.databinding.FragmentPracticeStatisticsBinding
import com.ruege.mobile.ui.adapter.StatisticsPagerAdapter
import com.ruege.mobile.ui.viewmodel.PracticeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat

@AndroidEntryPoint
class PracticeStatisticsFragment : Fragment() {

    private val viewModel: PracticeViewModel by activityViewModels()
    
    private var _binding: FragmentPracticeStatisticsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPracticeStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewPager()
        observeViewModel()
    }

    private fun setupViewPager() {
        val pagerAdapter = StatisticsPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "По номерам"
                1 -> "Последние попытки"
                2 -> "По вариантам"
                3 -> "Сочинения"
                else -> "Статистика"
            }
        }.attach()
    }

    private fun observeViewModel() {
        viewModel.totalAttempts.observe(viewLifecycleOwner) { count ->
            binding.tvAttemptCount.text = count.toString()
            updateSuccessRate()
        }
        
        viewModel.totalCorrectAttempts.observe(viewLifecycleOwner) { count ->
            binding.tvCorrectCount.text = count.toString()
            updateSuccessRate()
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNullOrEmpty()) {
                binding.tvError.visibility = View.GONE
            } else {
                binding.tvError.text = error
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }
    
    private fun updateSuccessRate() {
        val successRate = viewModel.calculateSuccessRate()
        if (successRate != null) {
            val formatter = DecimalFormat("0.0")
            binding.tvSuccessRate.text = "${formatter.format(successRate)}%"
            binding.progressOverall.progress = successRate.toInt()
            
            val colorResId = when {
                successRate >= 80 -> android.R.color.holo_green_dark
                successRate >= 50 -> android.R.color.holo_orange_dark
                else -> android.R.color.holo_red_dark
            }
            binding.progressOverall.progressTintList = android.content.res.ColorStateList.valueOf(
                requireContext().getColor(colorResId)
            )
        } else {
            binding.tvSuccessRate.text = "0.0%"
            binding.progressOverall.progress = 0
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 