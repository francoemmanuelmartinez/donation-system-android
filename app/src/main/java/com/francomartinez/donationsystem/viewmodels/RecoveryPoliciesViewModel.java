package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.RecoveryPolicy;
import com.francomartinez.donationsystem.repositories.RecoveryPoliciesRepository;
import com.francomartinez.donationsystem.utils.Resource;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecoveryPoliciesViewModel extends ViewModel {

    private final RecoveryPoliciesRepository repository;
    private final MutableLiveData<Resource<List<RecoveryPolicy>>> policies = new MutableLiveData<>();
    private final MutableLiveData<Resource<String>> operationMessage = new MutableLiveData<>();

    public RecoveryPoliciesViewModel() {
        this.repository = new RecoveryPoliciesRepository();
    }

    public LiveData<Resource<List<RecoveryPolicy>>> getPolicies() {
        return policies;
    }

    public LiveData<Resource<String>> getOperationMessage() {
        return operationMessage;
    }

    public void loadPolicies() {
        policies.postValue(Resource.loading());
        repository.getRecoveryPolicies(new Callback<ApiResponse<List<RecoveryPolicy>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RecoveryPolicy>>> call, Response<ApiResponse<List<RecoveryPolicy>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<RecoveryPolicy>> body = response.body();
                    if (body.isSuccess()) {
                        policies.postValue(Resource.success(body.getData()));
                    } else {
                        policies.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudieron recuperar las políticas"));
                    }
                } else {
                    policies.postValue(Resource.error("No se pudieron recuperar las políticas: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<RecoveryPolicy>>> call, Throwable t) {
                policies.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void createPolicy(RecoveryPolicy policy) {
        operationMessage.postValue(Resource.loading());
        repository.createRecoveryPolicy(policy, new Callback<ApiResponse<RecoveryPolicy>>() {
            @Override
            public void onResponse(Call<ApiResponse<RecoveryPolicy>> call, Response<ApiResponse<RecoveryPolicy>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    operationMessage.postValue(Resource.success("Política creada correctamente"));
                } else {
                    String message = response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage()
                            : "No se pudo completar la operación";
                    operationMessage.postValue(Resource.error(message));
                }
                loadPolicies();
            }

            @Override
            public void onFailure(Call<ApiResponse<RecoveryPolicy>> call, Throwable t) {
                operationMessage.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void updatePolicy(RecoveryPolicy policy) {
        operationMessage.postValue(Resource.loading());
        repository.updateRecoveryPolicy(policy, new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    operationMessage.postValue(Resource.success("Política actualizada correctamente"));
                } else {
                    String message = response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage()
                            : "No se pudo completar la operación";
                    operationMessage.postValue(Resource.error(message));
                }
                loadPolicies();
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                operationMessage.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void deletePolicy(int id) {
        operationMessage.postValue(Resource.loading());
        repository.deleteRecoveryPolicy(id, new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    operationMessage.postValue(Resource.success("Política eliminada correctamente"));
                } else {
                    String message = response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage()
                            : "No se pudo completar la operación";
                    operationMessage.postValue(Resource.error(message));
                }
                loadPolicies();
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                operationMessage.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }
}