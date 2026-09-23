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

public class LoginViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final MutableLiveData<Resource<LoginResponse>> loginResult = new MutableLiveData<>();

    public LoginViewModel() {
        this.userRepository = new UserRepository();
    }

    public LoginViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<Resource<LoginResponse>> getLoginResult() {
        return loginResult;
    }

    public void login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            loginResult.setValue(Resource.error("Please fill in all fields"));
            return;
        }

        loginResult.setValue(Resource.loading());

        userRepository.login(username, password, new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    if ("success".equals(body.getStatus())) {
                        loginResult.setValue(Resource.success(body));
                    } else {
                        loginResult.setValue(Resource.error(body.getMessage() != null ? body.getMessage() : "Login failed"));
                    }
                } else {
                    loginResult.setValue(Resource.error("Login failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                loginResult.setValue(Resource.error("Network error: " + t.getMessage()));
            }
        });
    }
}
