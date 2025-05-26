package com.ruege.mobile.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ruege.mobile.ui.fragment.RecentAttemptsFragment
import com.ruege.mobile.ui.fragment.StatisticsByTypeFragment

/**
 * Адаптер для ViewPager2, который отображает фрагменты с разными типами статистики
 */
class StatisticsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> StatisticsByTypeFragment()
            1 -> RecentAttemptsFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
} 