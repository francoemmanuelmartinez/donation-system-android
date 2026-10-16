package com.francomartinez.donationsystem.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String DEFAULT_BASE_URL = "http://10.0.2.2/donation-system-api/";
    private static String baseUrl = DEFAULT_BASE_URL;
    private static Retrofit retrofit = null;

    /**
     * Points Retrofit at a different base URL (used by integration tests).
     * Subsequent {@link #getApiService()} calls rebuild the Retrofit instance.
     */
    public static void setBaseUrl(String url) {
        baseUrl = (url == null) ? DEFAULT_BASE_URL : url;
        retrofit = null;
    }

    /** Restores the production base URL. */
    public static void reset() {
        setBaseUrl(DEFAULT_BASE_URL);
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
