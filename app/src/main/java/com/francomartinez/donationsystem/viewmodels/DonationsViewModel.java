package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.Donation;
import com.francomartinez.donationsystem.repositories.DonationRepository;
import com.francomartinez.donationsystem.utils.Resource;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DonationsViewModel extends ViewModel {

    private final DonationRepository donationRepository;
    private final MutableLiveData<Resource<Map<String, Object>>> saveResult = new MutableLiveData<>();

    public DonationsViewModel() {
        this.donationRepository = new DonationRepository();
    }

    public LiveData<Resource<Map<String, Object>>> getSaveResult() {
        return saveResult;
    }

    public void saveDonation(Donation donation) {
        saveResult.postValue(Resource.loading());
        donationRepository.saveDonation(donation, new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> body = response.body();
                    if (body.isSuccess()) {
                        saveResult.postValue(Resource.success(body.getData()));
                    } else {
                        saveResult.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudo registrar la donación"));
                    }
                } else {
                    saveResult.postValue(Resource.error("No se pudo registrar la donación: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                saveResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }
}