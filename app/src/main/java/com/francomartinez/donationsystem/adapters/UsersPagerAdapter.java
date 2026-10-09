package com.francomartinez.donationsystem.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.francomartinez.donationsystem.views.fragments.ActiveUsersFragment;
import com.francomartinez.donationsystem.views.fragments.PendingUsersFragment;

public class UsersPagerAdapter extends FragmentStateAdapter {

    public UsersPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return position == 0 ? new PendingUsersFragment() : new ActiveUsersFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}