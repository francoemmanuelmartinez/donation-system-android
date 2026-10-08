package com.francomartinez.donationsystem.views;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.viewmodels.RegisterViewModel;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;
    private RegisterViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        observeViewModel();

        btnRegister.setOnClickListener(v -> handleRegister());
        tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        viewModel.getRegisterResult().observe(this, resource -> {
            if (resource == null) return;

            switch (resource.status) {
                case LOADING:
                    btnRegister.setEnabled(false);
                    break;
                case SUCCESS:
                    btnRegister.setEnabled(true);
                    String message = resource.data;
                    if (message != null) {
                        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                    finish(); // Go back to LoginActivity
                    break;
                case ERROR:
                    btnRegister.setEnabled(true);
                    if (resource.message != null) {
                        Toast.makeText(RegisterActivity.this, resource.message, Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        });
    }

    private void handleRegister() {
        String input = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        viewModel.register("Usuario", "Staff", input, input, "", password);
    }
}
