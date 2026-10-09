package com.francomartinez.donationsystem.integration.support;

import com.francomartinez.donationsystem.api.ApiService;
import com.francomartinez.donationsystem.api.RetrofitClient;
import com.francomartinez.donationsystem.models.ApiResponse;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

/** Synchronous helper to drive the real API from JVM tests. */
public final class ApiTestClient {

    private ApiTestClient() {
    }

    public static ApiService api() {
        return RetrofitClient.getApiService();
    }

    /** Executes a call and returns the body, failing the test on non-2xx or missing body. */
    public static <T> ApiResponse<T> call(Call<ApiResponse<T>> call) throws IOException {
        Response<ApiResponse<T>> response = call.execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new AssertionError("Expected success but got HTTP " + response.code() + " for "
                    + call.request().url());
        }
        return response.body();
    }

    /** Executes a call and returns the raw HTTP status code. */
    public static <T> int status(Call<ApiResponse<T>> call) throws IOException {
        return call.execute().code();
    }
}