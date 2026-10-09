package com.francomartinez.donationsystem.repositories;

import com.francomartinez.donationsystem.api.ApiService;
import com.francomartinez.donationsystem.api.RetrofitClient;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.RecoveryPolicy;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;

public class RecoveryPoliciesRepository {

    private final ApiService apiService;

    public RecoveryPoliciesRepository() {
        apiService = RetrofitClient.getApiService();
    }

    public void getRecoveryPolicies(Callback<ApiResponse<List<RecoveryPolicy>>> callback) {
        Call<ApiResponse<List<RecoveryPolicy>>> call = apiService.getRecoveryPolicies();
        call.enqueue(callback);
    }

    public void createRecoveryPolicy(RecoveryPolicy policy, Callback<ApiResponse<RecoveryPolicy>> callback) {
        Call<ApiResponse<RecoveryPolicy>> call = apiService.createRecoveryPolicy(policy);
        call.enqueue(callback);
    }

    public void updateRecoveryPolicy(RecoveryPolicy policy, Callback<ApiResponse<Map<String, Object>>> callback) {
        Call<ApiResponse<Map<String, Object>>> call = apiService.updateRecoveryPolicy(policy);
        call.enqueue(callback);
    }

    public void deleteRecoveryPolicy(int id, Callback<ApiResponse<Map<String, Object>>> callback) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("id", id);
        Call<ApiResponse<Map<String, Object>>> call = apiService.deleteRecoveryPolicy(body);
        call.enqueue(callback);
    }
}