package com.francomartinez.donationsystem.api;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String DEFAULT_BASE_URL = "http://10.0.2.2/donation-system-api/";
    private static String baseUrl = DEFAULT_BASE_URL;
    private static Retrofit retrofit = null;
    private static int centerId = 0;

    /**
     * Sets the center ID to be sent as a query parameter in all requests.
     * Call this after login (from SessionManager).
     */
    public static void setCenterId(int id) {
        centerId = id;
        retrofit = null; // rebuild
    }

    public static int getCenterId() {
        return centerId;
    }

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
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            Interceptor centerInterceptor = chain -> {
                Request original = chain.request();
                if (centerId > 0) {
                    HttpUrl url = original.url().newBuilder()
                            .addQueryParameter("centerId", String.valueOf(centerId))
                            .build();
                    Request request = original.newBuilder().url(url).build();
                    return chain.proceed(request);
                }
                return chain.proceed(original);
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(centerInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
