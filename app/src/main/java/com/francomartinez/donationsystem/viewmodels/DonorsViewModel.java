package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.Donor;
import com.francomartinez.donationsystem.models.MedicalRestriction;
import com.francomartinez.donationsystem.models.requests.AssignRestrictionRequest;
import com.francomartinez.donationsystem.repositories.DonorRepository;
import com.francomartinez.donationsystem.utils.Resource;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DonorsViewModel extends ViewModel {

    private final DonorRepository donorRepository;
    private final MutableLiveData<Resource<Donor>> donorResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Map<String, Object>>> saveResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<MedicalRestriction>>> restrictionsResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Map<String, Object>>> assignResult = new MutableLiveData<>();

    public DonorsViewModel() {
        this.donorRepository = new DonorRepository();
    }

    public LiveData<Resource<Donor>> getDonorResult() {
        return donorResult;
    }

    public LiveData<Resource<Map<String, Object>>> getSaveResult() {
        return saveResult;
    }

    public LiveData<Resource<List<MedicalRestriction>>> getRestrictionsResult() {
        return restrictionsResult;
    }

    public LiveData<Resource<Map<String, Object>>> getAssignResult() {
        return assignResult;
    }

    public void loadRestrictions() {
        donorRepository.getRestrictionsDictionary(new Callback<ApiResponse<List<MedicalRestriction>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<MedicalRestriction>>> call, Response<ApiResponse<List<MedicalRestriction>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    restrictionsResult.postValue(Resource.success(response.body().getData()));
                } else {
                    restrictionsResult.postValue(Resource.error("No se pudieron cargar las restricciones médicas"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<MedicalRestriction>>> call, Throwable t) {
                restrictionsResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void getDonorByDni(String dni) {
        donorResult.postValue(Resource.loading());
        donorRepository.getDonorByDni(dni, new Callback<ApiResponse<Donor>>() {
            @Override
            public void onResponse(Call<ApiResponse<Donor>> call, Response<ApiResponse<Donor>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Donor> body = response.body();
                    if (body.isSuccess()) {
                        donorResult.postValue(Resource.success(body.getData()));
                    } else {
                        donorResult.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "Donante no encontrado"));
                    }
                } else {
                    donorResult.postValue(Resource.error("Donante no encontrado (HTTP " + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Donor>> call, Throwable t) {
                donorResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void getDonorById(int id) {
        donorResult.postValue(Resource.loading());
        donorRepository.getDonorById(id, new Callback<ApiResponse<Donor>>() {
            @Override
            public void onResponse(Call<ApiResponse<Donor>> call, Response<ApiResponse<Donor>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Donor> body = response.body();
                    if (body.isSuccess()) {
                        donorResult.postValue(Resource.success(body.getData()));
                    } else {
                        donorResult.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "Donante no encontrado"));
                    }
                } else {
                    donorResult.postValue(Resource.error("Donante no encontrado (HTTP " + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Donor>> call, Throwable t) {
                donorResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void saveDonor(Donor donor) {
        saveResult.postValue(Resource.loading());
        donorRepository.saveDonor(donor, new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> body = response.body();
                    if (body.isSuccess()) {
                        saveResult.postValue(Resource.success(body.getData()));
                    } else {
                        saveResult.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudo guardar el donante"));
                    }
                } else {
                    saveResult.postValue(Resource.error("No se pudo guardar el donante: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                saveResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void assignRestrictions(List<AssignRestrictionRequest> requests) {
        assignResult.postValue(Resource.loading());
        assignSequentially(requests, 0);
    }

    private void assignSequentially(List<AssignRestrictionRequest> requests, int index) {
        if (requests == null || requests.isEmpty() || index >= requests.size()) {
            assignResult.postValue(Resource.success(null));
            return;
        }

        donorRepository.assignRestriction(requests.get(index), new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    assignSequentially(requests, index + 1);
                } else {
                    assignResult.postValue(Resource.error("No se pudo asignar una restricción"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                assignResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }
}