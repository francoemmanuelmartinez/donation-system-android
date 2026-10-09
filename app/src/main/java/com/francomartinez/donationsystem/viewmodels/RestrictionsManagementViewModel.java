package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.MedicalRestriction;
import com.francomartinez.donationsystem.repositories.RestrictionsRepository;
import com.francomartinez.donationsystem.utils.Resource;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RestrictionsManagementViewModel extends ViewModel {

    private final RestrictionsRepository repository;
    private final MutableLiveData<Resource<List<MedicalRestriction>>> dictionary = new MutableLiveData<>();
    private final MutableLiveData<Resource<String>> operationMessage = new MutableLiveData<>();

    public RestrictionsManagementViewModel() {
        this.repository = new RestrictionsRepository();
    }

    public LiveData<Resource<List<MedicalRestriction>>> getDictionary() {
        return dictionary;
    }

    public LiveData<Resource<String>> getOperationMessage() {
        return operationMessage;
    }

    public void loadDictionary() {
        dictionary.postValue(Resource.loading());
        repository.getRestrictionsDictionary(new Callback<ApiResponse<List<MedicalRestriction>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<MedicalRestriction>>> call, Response<ApiResponse<List<MedicalRestriction>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<MedicalRestriction>> body = response.body();
                    if (body.isSuccess()) {
                        dictionary.postValue(Resource.success(body.getData()));
                    } else {
                        dictionary.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudo recuperar el diccionario"));
                    }
                } else {
                    dictionary.postValue(Resource.error("No se pudo recuperar el diccionario: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<MedicalRestriction>>> call, Throwable t) {
                dictionary.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void createRestriction(String name, int vedaDays) {
        operationMessage.postValue(Resource.loading());
        MedicalRestriction restriction = new MedicalRestriction();
        restriction.setName(name);
        restriction.setDefaultVedaDays(vedaDays);

        repository.createRestriction(restriction, new Callback<ApiResponse<MedicalRestriction>>() {
            @Override
            public void onResponse(Call<ApiResponse<MedicalRestriction>> call, Response<ApiResponse<MedicalRestriction>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    operationMessage.postValue(Resource.success("Exclusión creada correctamente"));
                } else {
                    String message = response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage()
                            : "No se pudo completar la operación";
                    operationMessage.postValue(Resource.error(message));
                }
                loadDictionary();
            }

            @Override
            public void onFailure(Call<ApiResponse<MedicalRestriction>> call, Throwable t) {
                operationMessage.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void deleteRestriction(int restrictionId) {
        operationMessage.postValue(Resource.loading());
        repository.deleteRestriction(restrictionId, new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    operationMessage.postValue(Resource.success("Exclusión eliminada correctamente"));
                } else {
                    String message = response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage()
                            : "No se pudo completar la operación";
                    operationMessage.postValue(Resource.error(message));
                }
                loadDictionary();
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                operationMessage.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }
}