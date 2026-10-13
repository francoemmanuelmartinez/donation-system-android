package com.francomartinez.donationsystem.views;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.views.fragments.AppointmentsFragment;
import com.francomartinez.donationsystem.views.fragments.DonationsFragment;
import com.francomartinez.donationsystem.views.fragments.DonorFormFragment;
import com.francomartinez.donationsystem.views.fragments.ScheduleAppointmentFragment;
import com.francomartinez.donationsystem.views.fragments.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        if (savedInstanceState == null) {
            showTab(DonorFormFragment.newRegisterInstance());
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int itemId = item.getItemId();
            if (itemId == R.id.navigationDonations) {
                fragment = DonorFormFragment.newRegisterInstance();
            } else if (itemId == R.id.navigationAppointments) {
                fragment = new AppointmentsFragment();
            } else {
                fragment = new SearchFragment();
            }
            showTab(fragment);
            return true;
        });
    }

    private void showTab(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fm.beginTransaction()
                .replace(R.id.fragmentContainerView, fragment)
                .commit();
    }

    private void push(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, fragment)
                .addToBackStack(fragment.getClass().getName())
                .commit();
    }

    public void openDonorDetails() {
        push(DonorFormFragment.newViewInstance());
    }

    public void openScheduleAppointment() {
        push(new ScheduleAppointmentFragment());
    }

    public void openClinicalForm() {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fm.beginTransaction()
                .replace(R.id.fragmentContainerView, DonorFormFragment.newRegisterInstance())
                .commit();
        push(new DonationsFragment());
    }
}