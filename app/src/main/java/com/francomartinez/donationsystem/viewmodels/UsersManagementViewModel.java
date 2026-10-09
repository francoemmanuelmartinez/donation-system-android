package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.User;
import com.francomartinez.donationsystem.repositories.UserAdminRepository;
import com.francomartinez.donationsystem.utils.ApiErrorHandler;
import com.francomartinez.donationsystem.utils.Resource;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsersManagementViewModel extends ViewModel {

    private final UserAdminRepository repository;
    private final MutableLiveData<Resource<List<User>>> pendingUsers = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<User>>> activeUsers = new MutableLiveData<>();
    private final MutableLiveData<Resource<String>> operationMessage = new MutableLiveData<>();

    public UsersManagementViewModel() {
        this.repository = new UserAdminRepository();
    }

    public LiveData<Resource<List<User>>> getPendingUsers() {
        return pendingUsers;
    }

    public LiveData<Resource<List<User>>> getActiveUsers() {
        return activeUsers;
    }

    public LiveData<Resource<String>> getOperationMessage() {
        return operationMessage;
    }

    public void loadPendingUsers() {
        pendingUsers.postValue(Resource.loading());
        repository.getPendingUsers(new Callback<ApiResponse<List<User>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<User>>> call, Response<ApiResponse<List<User>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<User>> body = response.body();
                    if (body.isSuccess()) {
                        pendingUsers.postValue(Resource.success(body.getData()));
                    } else {
                        pendingUsers.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudieron recuperar los usuarios"));
                    }
                } else {
                    pendingUsers.postValue(Resource.error("No se pudieron recuperar los usuarios: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {
                pendingUsers.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void loadActiveUsers() {
        activeUsers.postValue(Resource.loading());
        repository.getActiveUsers(new Callback<ApiResponse<List<User>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<User>>> call, Response<ApiResponse<List<User>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<User>> body = response.body();
                    if (body.isSuccess()) {
                        activeUsers.postValue(Resource.success(body.getData()));
                    } else {
                        activeUsers.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudieron recuperar los usuarios"));
                    }
                } else {
                    activeUsers.postValue(Resource.error("No se pudieron recuperar los usuarios: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {
                activeUsers.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void approveUser(User user, String role, Integer approvedByUserId) {
        operationMessage.postValue(Resource.loading());
        repository.approveUser(user.getId(), role, approvedByUserId, new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                publishMutationResult(response, "Usuario aprobado correctamente");
                loadPendingUsers();
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                operationMessage.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void updateUserRole(int userId, String role) {
        operationMessage.postValue(Resource.loading());
        repository.updateUserRole(userId, role, new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                publishMutationResult(response, "Rol actualizado correctamente");
                loadActiveUsers();
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                operationMessage.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void deleteUser(int userId) {
        operationMessage.postValue(Resource.loading());
        repository.deleteUser(userId, new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                publishMutationResult(response, "Usuario eliminado correctamente");
                loadActiveUsers();
                loadPendingUsers();
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                operationMessage.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    private void publishMutationResult(Response<ApiResponse<Map<String, Object>>> response, String successMessage) {
        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
            operationMessage.postValue(Resource.success(successMessage));
        } else {
            String message = ApiErrorHandler.messageFrom(response);
            if (message == null && response.body() != null) {
                message = response.body().getMessage();
            }
            operationMessage.postValue(Resource.error(message != null ? message : "No se pudo completar la operación"));
        }
    }
}