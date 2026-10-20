package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.User;
import com.francomartinez.donationsystem.repositories.ProfileRepository;
import com.francomartinez.donationsystem.utils.Resource;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileViewModel extends ViewModel {

    private final ProfileRepository repository;
    private final MutableLiveData<Resource<User>> profile = new MutableLiveData<>();
    private final MutableLiveData<Resource<String>> saveResult = new MutableLiveData<>();

    public ProfileViewModel() {
        this.repository = new ProfileRepository();
    }

    public LiveData<Resource<User>> getProfile() {
        return profile;
    }

    public LiveData<Resource<String>> getSaveResult() {
        return saveResult;
    }

    public void loadProfile(int userId) {
        profile.postValue(Resource.loading());
        repository.getProfile(userId, new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<User> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        profile.postValue(Resource.success(body.getData()));
                    } else {
                        profile.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudo cargar el perfil"));
                    }
                } else {
                    profile.postValue(Resource.error("No se pudo cargar el perfil: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                profile.postValue(Resource.error("Error de conexión: " + t.getMessage()));
            }
        });
    }

    public void saveProfile(int userId, String firstName, String lastName, String email, String phone) {
        saveResult.postValue(Resource.loading());
        repository.updateProfile(userId, firstName, lastName, email, phone, new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> body = response.body();
                    if (body.isSuccess()) {
                        saveResult.postValue(Resource.success(body.getMessage() != null ? body.getMessage() : "Perfil actualizado"));
                    } else {
                        saveResult.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudo actualizar el perfil"));
                    }
                } else {
                    saveResult.postValue(Resource.error("No se pudo actualizar el perfil: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                saveResult.postValue(Resource.error("Error de conexión: " + t.getMessage()));
            }
        });
    }
}
