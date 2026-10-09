package com.francomartinez.donationsystem.integration.support;

import com.francomartinez.donationsystem.api.RetrofitClient;

import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Base class for integration tests that exercise the real backend running on the
 * developer machine (XAMPP) through the Android Retrofit stack.
 */
public abstract class BaseApiTest {

    protected static final String API_BASE_URL = "http://localhost/donation-system-api/";

    @BeforeClass
    public static void pointRetrofitAtLocalhost() {
        RetrofitClient.setBaseUrl(API_BASE_URL);
    }

    @Before
    public void resetDatabase() throws Exception {
        TestDatabase.resetSchema();
    }
}