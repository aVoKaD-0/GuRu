package com.ruege.mobile.ui.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.ruege.mobile.ui.fragment.ShpargalkaFragment;
import com.ruege.mobile.ui.fragment.TasksFragment;
import com.ruege.mobile.ui.fragment.TheoryFragment;
import com.ruege.mobile.ui.fragment.VariantsFragment;
import com.ruege.mobile.ui.fragment.EssayFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @Override
    public int getItemCount() {
        return 5;
    }

    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new TheoryFragment();
            case 1:
                return new TasksFragment();
            case 2:
                return new EssayFragment();
            case 3:
                return new ShpargalkaFragment();
            case 4:
                return new VariantsFragment();
            default:
                throw new IllegalStateException("Invalid position: " + position);
        }
    }
} 