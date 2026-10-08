package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.repositories.UserRepository;
import com.francomartinez.donationsystem.utils.Resource;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final MutableLiveData<Resource<String>> registerResult = new MutableLiveData<>();

    public RegisterViewModel() {
        this.userRepository = new UserRepository();
    }

    public RegisterViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<Resource<String>> getRegisterResult() {
        return registerResult;
    }

    public void register(String firstName, String lastName, String dni, String email, String phone, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            registerResult.setValue(Resource.error("Please fill in all fields"));
            return;
        }

        registerResult.setValue(Resource.loading());

        userRepository.register(firstName, lastName, dni, email, phone, password, new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> body = response.body();
                    if (body.isSuccess()) {
                        registerResult.setValue(Resource.success(body.getMessage()));
                    } else {
                        registerResult.setValue(Resource.error(body.getMessage() != null ? body.getMessage() : "Registration failed"));
                    }
                } else {
                    registerResult.setValue(Resource.error("Registration failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                registerResult.setValue(Resource.error("Network error: " + t.getMessage()));
            }
        });
    }
}
