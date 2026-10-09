package com.francomartinez.donationsystem.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.francomartinez.donationsystem.views.fragments.StatisticsRangeFragment;

public class StatisticsPagerAdapter extends FragmentStateAdapter {

    private static final String[] RANGES = {"day", "week", "month", "year"};

    public StatisticsPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return StatisticsRangeFragment.newInstance(RANGES[position]);
    }

    @Override
    public int getItemCount() {
        return RANGES.length;
    }
}