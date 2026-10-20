package com.francomartinez.donationsystem.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.api.ApiService;
import com.francomartinez.donationsystem.api.RetrofitClient;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.utils.RoleMapper;
import com.francomartinez.donationsystem.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

        // Center selector for superadmin
        String role = session.getRole();
        if ("SUPERADMIN".equalsIgnoreCase(role)) {
            buttonProfile.setText("Mi Perfil / Centros");
            buttonProfile.setOnClickListener(v -> showCenterSelector());
        }

    }

    private void openModule(Class<?> activityClass) {
        startActivity(new Intent(this, activityClass));
    }

    private void openProfile() {
        startActivity(new Intent(this, ProfileActivity.class));
    }

    private void showCenterSelector() {
        ApiService api = RetrofitClient.getApiService();
        Map<String, Object> body = new HashMap<>();
        body.put("requesterRole", "SUPERADMIN");
        body.put("centerAction", "list");
        api.getCenters(body).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Map<String, Object>>>> call, Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Map<String, Object>> centers = response.body().getData();
                    if (centers == null || centers.isEmpty()) {
                        Toast.makeText(DirectorDashboardActivity.this, "No hay centros", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String[] names = new String[centers.size()];
                    final int[] ids = new int[centers.size()];
                    for (int i = 0; i < centers.size(); i++) {
                        Map<String, Object> c = centers.get(i);
                        Object name = c.get("name");
                        Object id = c.get("id");
                        names[i] = name != null ? name.toString() : "Centro " + i;
                        ids[i] = id instanceof Number ? ((Number) id).intValue() : 0;
                    }
                    new AlertDialog.Builder(DirectorDashboardActivity.this)
                            .setTitle("Seleccionar centro")
                            .setItems(names, (dialog, which) -> {
                                int selectedId = ids[which];
                                SessionManager.getInstance(DirectorDashboardActivity.this).setCenterId(selectedId);
                                Toast.makeText(DirectorDashboardActivity.this, "Centro: " + names[which], Toast.LENGTH_SHORT).show();
                            })
                            .show();
                } else {
                    Toast.makeText(DirectorDashboardActivity.this, "Error al cargar centros", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Map<String, Object>>>> call, Throwable t) {
                Toast.makeText(DirectorDashboardActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }
}