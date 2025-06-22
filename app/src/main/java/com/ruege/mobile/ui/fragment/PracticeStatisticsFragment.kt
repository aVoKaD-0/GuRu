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
import com.ruege.mobile.ui.adapter.StatisticsPagerAdapter
import com.ruege.mobile.ui.viewmodel.PracticeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat

@AndroidEntryPoint
class PracticeStatisticsFragment : Fragment() {

    private val viewModel: PracticeViewModel by activityViewModels()
    
    private lateinit var tvAttemptCount: TextView
    private lateinit var tvCorrectCount: TextView
    private lateinit var tvSuccessRate: TextView
    private lateinit var progressOverall: ProgressBar
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvError: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_practice_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tvAttemptCount = view.findViewById(R.id.tvAttemptCount)
        tvCorrectCount = view.findViewById(R.id.tvCorrectCount)
        tvSuccessRate = view.findViewById(R.id.tvSuccessRate)
        progressOverall = view.findViewById(R.id.progressOverall)
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)
        progressLoading = view.findViewById(R.id.progressLoading)
        tvError = view.findViewById(R.id.tvError)
        
        setupViewPager()
        observeViewModel()
    }

    private fun setupViewPager() {
        val pagerAdapter = StatisticsPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "По номерам"
                1 -> "Последние попытки"
                2 -> "По вариантам"
                else -> "Статистика"
            }
        }.attach()
    }

    private fun observeViewModel() {
        viewModel.totalAttempts.observe(viewLifecycleOwner) { count ->
            tvAttemptCount.text = count.toString()
            updateSuccessRate()
        }
        
        viewModel.totalCorrectAttempts.observe(viewLifecycleOwner) { count ->
            tvCorrectCount.text = count.toString()
            updateSuccessRate()
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNullOrEmpty()) {
                tvError.visibility = View.GONE
            } else {
                tvError.text = error
                tvError.visibility = View.VISIBLE
            }
        }
    }
    
    private fun updateSuccessRate() {
        val successRate = viewModel.calculateSuccessRate()
        if (successRate != null) {
            val formatter = DecimalFormat("0.0")
            tvSuccessRate.text = "${formatter.format(successRate)}%"
            progressOverall.progress = successRate.toInt()
            
            val colorResId = when {
                successRate >= 80 -> android.R.color.holo_green_dark
                successRate >= 50 -> android.R.color.holo_orange_dark
                else -> android.R.color.holo_red_dark
            }
            progressOverall.progressTintList = android.content.res.ColorStateList.valueOf(
                requireContext().getColor(colorResId)
            )
        } else {
            tvSuccessRate.text = "0.0%"
            progressOverall.progress = 0
        }
    }
} 