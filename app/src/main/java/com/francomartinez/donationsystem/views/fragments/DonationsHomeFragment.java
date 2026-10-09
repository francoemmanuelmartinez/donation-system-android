package com.francomartinez.donationsystem.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.views.MainActivity;
import com.google.android.material.button.MaterialButton;

public class DonationsHomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_donations_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton buttonRegisterNewDonor = view.findViewById(R.id.buttonRegisterNewDonor);
        MaterialButton buttonSearchRegisteredDonor = view.findViewById(R.id.buttonSearchRegisteredDonor);

        buttonRegisterNewDonor.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openDonorRegister();
            }
        });

        buttonSearchRegisteredDonor.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openDonorSearch();
            }
        });
    }
}