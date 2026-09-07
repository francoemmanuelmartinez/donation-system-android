package com.francomartinez.donationsystem.controllers;

import com.francomartinez.donationsystem.models.LoginResponse;
import com.francomartinez.donationsystem.repositories.UserRepository;

import retrofit2.Callback;

public class LoginController {
    private UserRepository userRepository;

    public LoginController() {
        userRepository = new UserRepository();
    }

    public void login(String username, String password, Callback<LoginResponse> callback) {
        userRepository.login(username, password, callback);
    }
}
