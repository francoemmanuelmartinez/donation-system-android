package com.francomartinez.donationsystem.repositories;

import com.francomartinez.donationsystem.api.ApiService;
import com.francomartinez.donationsystem.api.RetrofitClient;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.MedicalRestriction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;

public class RestrictionsRepository {

    private final ApiService apiService;

    public RestrictionsRepository() {
        apiService = RetrofitClient.getApiService();
    }

    public void getRestrictionsDictionary(Callback<ApiResponse<List<MedicalRestriction>>> callback) {
        Call<ApiResponse<List<MedicalRestriction>>> call = apiService.getRestrictionsDictionary();
        call.enqueue(callback);
    }

    public void createRestriction(MedicalRestriction restriction, Callback<ApiResponse<MedicalRestriction>> callback) {
        Call<ApiResponse<MedicalRestriction>> call = apiService.createRestriction(restriction);
        call.enqueue(callback);
    }

    public void deleteRestriction(int restrictionId, Callback<ApiResponse<Map<String, Object>>> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("restrictionId", restrictionId);
        Call<ApiResponse<Map<String, Object>>> call = apiService.deleteRestriction(body);
        call.enqueue(callback);
    }
}