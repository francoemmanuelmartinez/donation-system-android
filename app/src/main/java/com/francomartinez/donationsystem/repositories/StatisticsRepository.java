package com.francomartinez.donationsystem.repositories;

import com.francomartinez.donationsystem.api.ApiService;
import com.francomartinez.donationsystem.api.RetrofitClient;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.Statistics;

import retrofit2.Call;
import retrofit2.Callback;

public class StatisticsRepository {

    private final ApiService apiService;

    public StatisticsRepository() {
        apiService = RetrofitClient.getApiService();
    }

    public void getStatistics(String range, Callback<ApiResponse<Statistics>> callback) {
        Call<ApiResponse<Statistics>> call = apiService.getStatistics(range);
        call.enqueue(callback);
    }
}