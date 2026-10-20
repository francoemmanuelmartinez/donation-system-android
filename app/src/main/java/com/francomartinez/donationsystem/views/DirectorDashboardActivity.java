package com.francomartinez.donationsystem.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class DirectorDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_director_dashboard);

        SessionManager session = SessionManager.getInstance(this);
        TextView textViewDirectorName = findViewById(R.id.textViewDirectorName);
        textViewDirectorName.setText(session.getFullName());

        MaterialCardView cardMenuDonations = findViewById(R.id.cardMenuDonations);
        cardMenuDonations.setOnClickListener(v -> openModule(MainActivity.class));
        MaterialCardView cardMenuStatistics = findViewById(R.id.cardMenuStatistics);
        cardMenuStatistics.setOnClickListener(v -> openModule(StatisticsActivity.class));
        MaterialCardView cardMenuRestrictions = findViewById(R.id.cardMenuRestrictions);
        cardMenuRestrictions.setOnClickListener(v -> openModule(RestrictionsActivity.class));
        MaterialCardView cardMenuRecoveryPolicies = findViewById(R.id.cardMenuRecoveryPolicies);
        cardMenuRecoveryPolicies.setOnClickListener(v -> openModule(RecoveryPoliciesActivity.class));
        MaterialCardView cardMenuUsers = findViewById(R.id.cardMenuUsers);
        cardMenuUsers.setOnClickListener(v -> openModule(UsersActivity.class));
MaterialButton buttonProfile = findViewById(R.id.buttonProfile);
        buttonProfile.setOnClickListener(v -> openProfile());

    }

    private void openModule(Class<?> activityClass) {
        startActivity(new Intent(this, activityClass));
    }

    private void openProfile() {
        startActivity(new Intent(this, ProfileActivity.class));
    }
}