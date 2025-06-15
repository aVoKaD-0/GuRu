package com.ruege.mobile.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ruege.mobile.ui.fragment.RecentAttemptsFragment
import com.ruege.mobile.ui.fragment.StatisticsByTypeFragment
import com.ruege.mobile.ui.fragment.StatisticsByVariantFragment

/**
 * Адаптер для ViewPager2, который отображает фрагменты с разными типами статистики
 */
class StatisticsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> StatisticsByTypeFragment()
            1 -> RecentAttemptsFragment()
            2 -> StatisticsByVariantFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
} 