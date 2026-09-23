package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.LoginResponse;
import com.francomartinez.donationsystem.repositories.UserRepository;
import com.francomartinez.donationsystem.utils.Resource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final MutableLiveData<Resource<LoginResponse>> registerResult = new MutableLiveData<>();

    public RegisterViewModel() {
        this.userRepository = new UserRepository();
    }

    public RegisterViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<Resource<LoginResponse>> getRegisterResult() {
        return registerResult;
    }

    public void register(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            registerResult.setValue(Resource.error("Please fill in all fields"));
            return;
        }

        registerResult.setValue(Resource.loading());

        userRepository.register(username, password, new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    registerResult.setValue(Resource.success(body));
                } else {
                    registerResult.setValue(Resource.error("Registration failed"));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                registerResult.setValue(Resource.error("Network error: " + t.getMessage()));
            }
        });
    }
}
