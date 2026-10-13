package com.francomartinez.donationsystem.repositories;

import com.francomartinez.donationsystem.api.ApiService;
import com.francomartinez.donationsystem.api.RetrofitClient;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.SearchPage;

import retrofit2.Call;
import retrofit2.Callback;

public class SearchRepository {

    private final ApiService apiService;

    public SearchRepository() {
        apiService = RetrofitClient.getApiService();
    }

    public void search(String q, String group, String factor,
                       int limit, int offset,
                       Callback<ApiResponse<SearchPage>> callback) {
        Call<ApiResponse<SearchPage>> call = apiService.searchDonors(
                emptyIfNull(q),
                emptyIfNull(group),
                emptyIfNull(factor),
                limit,
                offset);
        call.enqueue(callback);
    }

    private static String emptyIfNull(String value) {
        return value != null ? value : "";
    }
}
