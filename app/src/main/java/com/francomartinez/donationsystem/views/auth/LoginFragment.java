package com.francomartinez.donationsystem.views.auth;

import android.content.Intent;
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
import com.francomartinez.donationsystem.models.User;
import com.francomartinez.donationsystem.viewmodels.LoginViewModel;
import com.francomartinez.donationsystem.views.DirectorDashboardActivity;
import com.francomartinez.donationsystem.views.MainActivity;
import com.francomartinez.donationsystem.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginFragment extends Fragment {

    private TextInputEditText editTextLoginEmail;
    private TextInputEditText editTextLoginPassword;
    private MaterialButton buttonLogin;
    private MaterialButton buttonNavigateToSignUp;
    private LoginViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        editTextLoginEmail = view.findViewById(R.id.editTextLoginEmail);
        editTextLoginPassword = view.findViewById(R.id.editTextLoginPassword);
        buttonLogin = view.findViewById(R.id.buttonLogin);
        buttonNavigateToSignUp = view.findViewById(R.id.buttonNavigateToSignUp);

        observeViewModel();

        buttonLogin.setOnClickListener(v -> handleLogin());
        buttonNavigateToSignUp.setOnClickListener(v -> {
            if (requireActivity() instanceof AuthActivity) {
                ((AuthActivity) requireActivity()).navigateToSignUp();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getLoginResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            switch (resource.status) {
                case LOADING:
                    buttonLogin.setEnabled(false);
                    break;
                case SUCCESS:
                    buttonLogin.setEnabled(true);
                    routeByRole(resource.data);
                    break;
                case ERROR:
                    buttonLogin.setEnabled(true);
                    if (resource.message != null) {
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        });
    }

    private void routeByRole(User user) {
        if (user == null) {
            Toast.makeText(requireContext(), R.string.msg_login_error, Toast.LENGTH_LONG).show();
            return;
        }

        SessionManager.getInstance(requireContext()).saveUser(user);
        Toast.makeText(requireContext(), getString(R.string.msg_welcome, user.getFirstName()), Toast.LENGTH_SHORT).show();

        String role = user.getRole();
        Intent intent;
        if (role != null && (role.equalsIgnoreCase("DIRECTORA") || role.equalsIgnoreCase("DIRECTOR") || role.equalsIgnoreCase("SUPERADMIN"))) {
            intent = new Intent(requireContext(), DirectorDashboardActivity.class);
        } else {
            intent = new Intent(requireContext(), MainActivity.class);
        }

        startActivity(intent);
        requireActivity().finish();
    }

    private void handleLogin() {
        String email = editTextLoginEmail.getText() != null ? editTextLoginEmail.getText().toString().trim() : "";
        String password = editTextLoginPassword.getText() != null ? editTextLoginPassword.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), R.string.msg_fill_all_fields, Toast.LENGTH_LONG).show();
            return;
        }

        viewModel.login(email, password);
    }
}
