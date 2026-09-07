package com.francomartinez.donationsystem.controllers;

import com.francomartinez.donationsystem.models.LoginResponse;
import com.francomartinez.donationsystem.repositories.UserRepository;

import retrofit2.Callback;

public class RegisterController {
    private UserRepository userRepository;

    public RegisterController() {
        userRepository = new UserRepository();
    }

    public void register(String username, String password, Callback<LoginResponse> callback) {
        userRepository.register(username, password, callback);
    }
}
