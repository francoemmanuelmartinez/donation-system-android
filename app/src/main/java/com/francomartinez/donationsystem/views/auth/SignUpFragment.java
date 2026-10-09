package com.francomartinez.donationsystem.views.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.viewmodels.RegisterViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class SignUpFragment extends Fragment {

    private TextInputEditText editTextFirstName;
    private TextInputEditText editTextLastName;
    private TextInputEditText editTextDni;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPhone;
    private TextInputEditText editTextPassword;
    private MaterialButton buttonSignUp;
    private MaterialButton buttonNavigateToLogin;
    private RegisterViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        editTextFirstName = view.findViewById(R.id.editTextFirstName);
        editTextLastName = view.findViewById(R.id.editTextLastName);
        editTextDni = view.findViewById(R.id.editTextDni);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPhone = view.findViewById(R.id.editTextPhone);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        buttonSignUp = view.findViewById(R.id.buttonSignUp);
        buttonNavigateToLogin = view.findViewById(R.id.buttonNavigateToLogin);

        observeViewModel();

        buttonSignUp.setOnClickListener(v -> handleRegister());
        buttonNavigateToLogin.setOnClickListener(v -> {
            if (requireActivity() instanceof AuthActivity) {
                ((AuthActivity) requireActivity()).navigateToLogin();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getRegisterResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            switch (resource.status) {
                case LOADING:
                    buttonSignUp.setEnabled(false);
                    break;
                case SUCCESS:
                    buttonSignUp.setEnabled(true);
                    Toast.makeText(requireContext(), R.string.msg_registration_success, Toast.LENGTH_LONG).show();
                    if (requireActivity() instanceof AuthActivity) {
                        ((AuthActivity) requireActivity()).navigateToLogin();
                    }
                    break;
                case ERROR:
                    buttonSignUp.setEnabled(true);
                    if (resource.message != null) {
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        });
    }

    private void handleRegister() {
        String firstName = textOf(editTextFirstName);
        String lastName = textOf(editTextLastName);
        String dni = textOf(editTextDni);
        String email = textOf(editTextEmail);
        String phone = textOf(editTextPhone);
        String password = textOf(editTextPassword);

        if (firstName.isEmpty() || lastName.isEmpty() || dni.isEmpty()
                || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), R.string.msg_fill_all_fields, Toast.LENGTH_LONG).show();
            return;
        }

        viewModel.register(firstName, lastName, dni, email, phone, password);
    }

    private String textOf(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}
