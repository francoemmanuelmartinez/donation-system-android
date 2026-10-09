package com.francomartinez.donationsystem.repositories;

import com.francomartinez.donationsystem.api.ApiService;
import com.francomartinez.donationsystem.api.RetrofitClient;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.Donation;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;

public class DonationRepository {

    private final ApiService apiService;

    public DonationRepository() {
        apiService = RetrofitClient.getApiService();
    }

    public void saveDonation(Donation donation, Callback<ApiResponse<Map<String, Object>>> callback) {
        Call<ApiResponse<Map<String, Object>>> call = apiService.saveDonation(donation);
        call.enqueue(callback);
    }
}