package com.francomartinez.donationsystem.repositories;

import com.francomartinez.donationsystem.api.ApiService;
import com.francomartinez.donationsystem.api.RetrofitClient;
import com.francomartinez.donationsystem.models.LoginResponse;
import com.francomartinez.donationsystem.models.User;

import retrofit2.Call;
import retrofit2.Callback;

public class UserRepository {
    private ApiService apiService;

    public UserRepository() {
        apiService = RetrofitClient.getApiService();
    }

    public void login(String username, String password, Callback<LoginResponse> callback) {
        User user = new User(username, password);
        Call<LoginResponse> call = apiService.login(user);
        call.enqueue(callback);
    }

    public void register(String username, String password, Callback<LoginResponse> callback) {
        User user = new User(username, password);
        Call<LoginResponse> call = apiService.register(user);
        call.enqueue(callback);
    }
}
