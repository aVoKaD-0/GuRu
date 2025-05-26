package com.ruege.mobile.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ruege.mobile.ui.bottomsheet.PracticeStatisticsBottomSheetDialogFragment
import com.ruege.mobile.ui.fragment.RecentAttemptsFragment
import com.ruege.mobile.ui.fragment.StatisticsByNumberFragment

class PracticeStatisticsPagerAdapter(fragment: PracticeStatisticsBottomSheetDialogFragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> StatisticsByNumberFragment() // Фрагмент для статистики по номерам
            1 -> RecentAttemptsFragment()   // Фрагмент для последних попыток
            else -> throw IllegalStateException("Invalid position for ViewPager: $position")
        }
    }
} 