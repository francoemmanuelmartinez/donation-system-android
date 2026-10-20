package com.francomartinez.donationsystem.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.utils.RoleMapper;
import com.francomartinez.donationsystem.utils.SessionManager;
import com.francomartinez.donationsystem.viewmodels.ProfileViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;
    private SessionManager session;

    private TextInputEditText editTextFirstName;
    private TextInputEditText editTextLastName;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPhone;
    private TextView textViewRole;
    private TextView textViewCenter;
    private MaterialButton buttonSave;

    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        session = SessionManager.getInstance(requireContext());
        userId = session.getUserId();
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        editTextFirstName = view.findViewById(R.id.editTextProfileFirstName);
        editTextLastName = view.findViewById(R.id.editTextProfileLastName);
        editTextEmail = view.findViewById(R.id.editTextProfileEmail);
        editTextPhone = view.findViewById(R.id.editTextProfilePhone);
        textViewRole = view.findViewById(R.id.textViewProfileRole);
        textViewCenter = view.findViewById(R.id.textViewProfileCenter);
        buttonSave = view.findViewById(R.id.buttonSaveProfile);

        // Pre-fill from session
        editTextFirstName.setText(session.getFirstName());
        editTextLastName.setText(session.getFullName().replace(session.getFirstName(), "").trim());
        editTextEmail.setText(session.getEmail());

        textViewRole.setText("Rol: " + RoleMapper.toSpanish(session.getRole()));

        Integer centerId = session.getCenterId();
        textViewCenter.setText("Centro: " + (centerId != null ? "#" + centerId : "Sin asignar"));

        buttonSave.setOnClickListener(v -> saveProfile());

        observeViewModel();
        viewModel.loadProfile(userId);
    }

    private void observeViewModel() {
        viewModel.getProfile().observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            if (res.status == Resource.Status.SUCCESS && res.data != null) {
                editTextFirstName.setText(res.data.getFirstName());
                editTextLastName.setText(res.data.getLastName());
                editTextEmail.setText(res.data.getEmail());
                if (res.data.getPhones() != null && !res.data.getPhones().isEmpty()) {
                    editTextPhone.setText(res.data.getPhones().get(0).getPhoneNumber());
                }
                textViewRole.setText("Rol: " + RoleMapper.toSpanish(res.data.getRole()));
                if (res.data.getCenterId() != null) {
                    textViewCenter.setText("Centro: #" + res.data.getCenterId());
                }
            } else if (res.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getSaveResult().observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            if (res.status == Resource.Status.SUCCESS) {
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_SHORT).show();
                // Reload profile to refresh session
                viewModel.loadProfile(userId);
            } else if (res.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveProfile() {
        String firstName = editTextFirstName.getText() != null ? editTextFirstName.getText().toString().trim() : "";
        String lastName = editTextLastName.getText() != null ? editTextLastName.getText().toString().trim() : "";
        String email = editTextEmail.getText() != null ? editTextEmail.getText().toString().trim() : "";
        String phone = editTextPhone.getText() != null ? editTextPhone.getText().toString().trim() : "";

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.saveProfile(userId, firstName, lastName, email, phone);
    }
}
