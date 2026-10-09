package com.francomartinez.donationsystem.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.adapters.UsersPagerAdapter;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.viewmodels.UsersManagementViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class UsersManagementFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_users_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        UsersManagementViewModel viewModel = new ViewModelProvider(requireActivity()).get(UsersManagementViewModel.class);

        TabLayout tabLayoutUsers = view.findViewById(R.id.tabLayoutUsers);
        ViewPager2 viewPagerUsers = view.findViewById(R.id.viewPagerUsers);

        viewPagerUsers.setAdapter(new UsersPagerAdapter(this));
        new TabLayoutMediator(tabLayoutUsers, viewPagerUsers, (tab, position) -> {
            tab.setText(position == 0 ? R.string.tab_pending_users : R.string.tab_active_users);
        }).attach();

        viewModel.getOperationMessage().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) {
                return;
            }
            if (resource.status == Resource.Status.SUCCESS) {
                Toast.makeText(requireContext(), resource.data, Toast.LENGTH_LONG).show();
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
            }
        });
    }
}