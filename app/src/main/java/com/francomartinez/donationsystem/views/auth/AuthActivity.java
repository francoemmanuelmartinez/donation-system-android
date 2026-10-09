package com.francomartinez.donationsystem.views.auth;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.francomartinez.donationsystem.R;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.authFragmentContainer, new LoginFragment())
                    .commit();
        }
    }

    public void navigateToSignUp() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.authFragmentContainer, new SignUpFragment())
                .addToBackStack(null)
                .commit();
    }

    public void navigateToLogin() {
        getSupportFragmentManager().popBackStack();
    }
}
