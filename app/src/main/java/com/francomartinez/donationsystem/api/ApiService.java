package com.francomartinez.donationsystem.api;

import com.francomartinez.donationsystem.models.LoginResponse;
import com.francomartinez.donationsystem.models.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("login.php")
    Call<LoginResponse> login(@Body User user);

    @POST("register.php")
    Call<LoginResponse> register(@Body User user);
}
