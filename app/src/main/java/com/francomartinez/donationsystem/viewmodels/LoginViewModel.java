package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.User;
import com.francomartinez.donationsystem.repositories.UserRepository;
import com.francomartinez.donationsystem.utils.Resource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final MutableLiveData<Resource<User>> loginResult = new MutableLiveData<>();

    public LoginViewModel() {
        this.userRepository = new UserRepository();
    }

    public LoginViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<Resource<User>> getLoginResult() {
        return loginResult;
    }

    public void login(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            loginResult.setValue(Resource.error("Please fill in all fields"));
            return;
        }

        loginResult.setValue(Resource.loading());

        userRepository.login(email, password, new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<User> body = response.body();
                    if (body.isSuccess()) {
                        loginResult.setValue(Resource.success(body.getData()));
                    } else {
                        loginResult.setValue(Resource.error(body.getMessage() != null ? body.getMessage() : "Login failed"));
                    }
                } else {
                    loginResult.setValue(Resource.error("Login failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                loginResult.setValue(Resource.error("Network error: " + t.getMessage()));
            }
        });
    }
}
