package com.francomartinez.donationsystem.repositories;

import com.francomartinez.donationsystem.api.ApiService;
import com.francomartinez.donationsystem.api.RetrofitClient;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.Donor;
import com.francomartinez.donationsystem.models.MedicalRestriction;
import com.francomartinez.donationsystem.models.SearchResultItem;
import com.francomartinez.donationsystem.models.requests.AssignRestrictionRequest;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;

public class DonorRepository {

    private final ApiService apiService;

    public DonorRepository() {
        apiService = RetrofitClient.getApiService();
    }

    public void getDonorByDni(String dni, Callback<ApiResponse<Donor>> callback) {
        Call<ApiResponse<Donor>> call = apiService.getDonorByDni(dni);
        call.enqueue(callback);
    }

    public void saveDonor(Donor donor, Callback<ApiResponse<Map<String, Object>>> callback) {
        Call<ApiResponse<Map<String, Object>>> call = apiService.saveDonor(donor);
        call.enqueue(callback);
    }

    public void searchDonorsByDni(String dni, Callback<ApiResponse<List<SearchResultItem>>> callback) {
        Call<ApiResponse<List<SearchResultItem>>> call = apiService.searchDonorsByDni(dni);
        call.enqueue(callback);
    }

    public void getRestrictionsDictionary(Callback<ApiResponse<List<MedicalRestriction>>> callback) {
        Call<ApiResponse<List<MedicalRestriction>>> call = apiService.getRestrictionsDictionary();
        call.enqueue(callback);
    }

    public void assignRestriction(AssignRestrictionRequest request, Callback<ApiResponse<Map<String, Object>>> callback) {
        Call<ApiResponse<Map<String, Object>>> call = apiService.assignDonorRestriction(request);
        call.enqueue(callback);
    }
}