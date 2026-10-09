package com.francomartinez.donationsystem.repositories;

import com.francomartinez.donationsystem.api.ApiService;
import com.francomartinez.donationsystem.api.RetrofitClient;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.SearchResultItem;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

public class SearchRepository {

    private final ApiService apiService;

    public SearchRepository() {
        apiService = RetrofitClient.getApiService();
    }

    public void searchByBloodType(String bloodType, Callback<ApiResponse<List<SearchResultItem>>> callback) {
        Call<ApiResponse<List<SearchResultItem>>> call = apiService.searchDonorsByBloodType(bloodType);
        call.enqueue(callback);
    }
}