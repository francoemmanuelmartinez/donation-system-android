package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.User;
import com.francomartinez.donationsystem.repositories.UserRepository;
import com.francomartinez.donationsystem.utils.ApiErrorHandler;
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
            loginResult.postValue(Resource.error("Por favor completá todos los campos"));
            return;
        }

        loginResult.postValue(Resource.loading());

        userRepository.login(email, password, new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<User> body = response.body();
                    if (body.isSuccess()) {
                        loginResult.postValue(Resource.success(body.getData()));
                    } else {
                        loginResult.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudo iniciar sesión"));
                    }
                } else {
                    String message = ApiErrorHandler.messageFrom(response);
                    loginResult.postValue(Resource.error(message != null ? message : "No se pudo iniciar sesión: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                loginResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }
}
