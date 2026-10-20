package com.francomartinez.donationsystem.repositories;

import com.francomartinez.donationsystem.api.ApiService;
import com.francomartinez.donationsystem.api.RetrofitClient;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.User;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;

public class ProfileRepository {

    private ApiService apiService;

    public ProfileRepository() {
        apiService = RetrofitClient.getApiService();
    }

    public void getProfile(int userId, Callback<ApiResponse<User>> callback) {
        Call<ApiResponse<User>> call = apiService.getProfile(userId);
        call.enqueue(callback);
    }

    public void updateProfile(int userId, String firstName, String lastName, String email, String phone, Callback<ApiResponse<Map<String, Object>>> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("email", email);
        body.put("phone", phone);
        Call<ApiResponse<Map<String, Object>>> call = apiService.updateProfile(body);
        call.enqueue(callback);
    }
}
