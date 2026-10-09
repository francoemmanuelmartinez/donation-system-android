package com.francomartinez.donationsystem.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.adapters.StatisticsPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class StatisticsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TabLayout tabStatistics = view.findViewById(R.id.tabStatistics);
        ViewPager2 viewPagerStatistics = view.findViewById(R.id.viewPagerStatistics);

        viewPagerStatistics.setAdapter(new StatisticsPagerAdapter(this));
        new TabLayoutMediator(tabStatistics, viewPagerStatistics, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.tab_statistics_day);
                    break;
                case 1:
                    tab.setText(R.string.tab_statistics_week);
                    break;
                case 2:
                    tab.setText(R.string.tab_statistics_month);
                    break;
                default:
                    tab.setText(R.string.tab_statistics_year);
                    break;
            }
        }).attach();
    }
}