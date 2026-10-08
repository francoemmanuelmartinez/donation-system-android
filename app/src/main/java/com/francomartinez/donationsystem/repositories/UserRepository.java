package com.francomartinez.donationsystem.repositories;

import com.francomartinez.donationsystem.api.ApiService;
import com.francomartinez.donationsystem.api.RetrofitClient;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.User;
import com.francomartinez.donationsystem.models.requests.LoginRequest;
import com.francomartinez.donationsystem.models.requests.RegisterRequest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;

public class UserRepository {
    private ApiService apiService;

    public UserRepository() {
        apiService = RetrofitClient.getApiService();
    }

    public void login(String email, String password, Callback<ApiResponse<User>> callback) {
        LoginRequest request = new LoginRequest(email, password);
        Call<ApiResponse<User>> call = apiService.login(request);
        call.enqueue(callback);
    }

    public void register(String firstName, String lastName, String dni, String email, String phone, String password, Callback<ApiResponse<Map<String, Object>>> callback) {
        RegisterRequest request = new RegisterRequest(firstName, lastName, dni, email, phone, password);
        Call<ApiResponse<Map<String, Object>>> call = apiService.register(request);
        call.enqueue(callback);
    }
}
