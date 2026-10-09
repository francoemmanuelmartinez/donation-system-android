package com.francomartinez.donationsystem.repositories;

import com.francomartinez.donationsystem.api.ApiService;
import com.francomartinez.donationsystem.api.RetrofitClient;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.User;
import com.francomartinez.donationsystem.models.requests.ApproveUserRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;

public class UserAdminRepository {

    private final ApiService apiService;

    public UserAdminRepository() {
        apiService = RetrofitClient.getApiService();
    }

    public void getPendingUsers(Callback<ApiResponse<List<User>>> callback) {
        Call<ApiResponse<List<User>>> call = apiService.getPendingUsers();
        call.enqueue(callback);
    }

    public void getActiveUsers(Callback<ApiResponse<List<User>>> callback) {
        Call<ApiResponse<List<User>>> call = apiService.getActiveUsers();
        call.enqueue(callback);
    }

    public void approveUser(int userId, String role, Integer approvedByUserId, Callback<ApiResponse<Map<String, Object>>> callback) {
        Call<ApiResponse<Map<String, Object>>> call = apiService.approveUser(new ApproveUserRequest(userId, role, approvedByUserId));
        call.enqueue(callback);
    }

    public void updateUserRole(int userId, String role, Callback<ApiResponse<Map<String, Object>>> callback) {
        Call<ApiResponse<Map<String, Object>>> call = apiService.updateUserRole(new ApproveUserRequest(userId, role, null));
        call.enqueue(callback);
    }

    public void deleteUser(int userId, Callback<ApiResponse<Map<String, Object>>> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        Call<ApiResponse<Map<String, Object>>> call = apiService.deleteUser(body);
        call.enqueue(callback);
    }
}