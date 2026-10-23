package com.francomartinez.donationsystem.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.adapters.DashboardAdapter;
import com.francomartinez.donationsystem.api.ApiService;
import com.francomartinez.donationsystem.api.RetrofitClient;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.DashboardModule;
import com.francomartinez.donationsystem.utils.DashboardModules;
import com.francomartinez.donationsystem.utils.RoleMapper;
import com.francomartinez.donationsystem.utils.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity implements DashboardAdapter.OnModuleClickListener {

    private DashboardAdapter dashboardAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashboardRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SessionManager session = SessionManager.getInstance(this);
        String role = session.getRole();

        TextView textViewDashboardRole = findViewById(R.id.textViewDashboardRole);
        textViewDashboardRole.setText(RoleMapper.toSpanish(role));
        TextView textViewDashboardName = findViewById(R.id.textViewDashboardName);
        textViewDashboardName.setText(session.getFullName());

        findViewById(R.id.buttonProfile).setOnClickListener(v -> openProfile());

        RecyclerView recyclerDashboardModules = findViewById(R.id.recyclerDashboardModules);
        recyclerDashboardModules.setLayoutManager(new GridLayoutManager(this, 2));
        dashboardAdapter = new DashboardAdapter(this);
        recyclerDashboardModules.setAdapter(dashboardAdapter);
        dashboardAdapter.setModules(DashboardModules.forRole(role));
    }

    @Override
    public void onModuleClick(DashboardModule module) {
        if (module.getTargetActivity() != null) {
            startActivity(new Intent(this, module.getTargetActivity()));
        } else if (DashboardModule.ACTION_CENTERS.equals(module.getAction())) {
            showCenterSelector();
        }
    }

    private void openProfile() {
        startActivity(new Intent(this, ProfileActivity.class));
    }

    private void showCenterSelector() {
        ApiService api = RetrofitClient.getApiService();
        Map<String, Object> body = new HashMap<>();
        body.put("requesterRole", RoleMapper.ROLE_SUPERADMIN);
        body.put("centerAction", "list");
        api.getCenters(body).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Map<String, Object>>>> call, Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Map<String, Object>> centers = response.body().getData();
                    if (centers == null || centers.isEmpty()) {
                        Toast.makeText(DashboardActivity.this, R.string.msg_no_centers, Toast.LENGTH_SHORT).show();
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
                    new AlertDialog.Builder(DashboardActivity.this)
                            .setTitle(R.string.msg_select_center)
                            .setItems(names, (dialog, which) -> {
                                int selectedId = ids[which];
                                SessionManager.getInstance(DashboardActivity.this).setCenterId(selectedId);
                                Toast.makeText(DashboardActivity.this,
                                        getString(R.string.msg_selected_center, names[which]),
                                        Toast.LENGTH_SHORT).show();
                            })
                            .show();
                } else {
                    Toast.makeText(DashboardActivity.this, R.string.msg_centers_load_error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Map<String, Object>>>> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, R.string.msg_connection_error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
