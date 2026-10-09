package com.francomartinez.donationsystem.integration.vm;

import androidx.lifecycle.LiveData;

import com.francomartinez.donationsystem.utils.Resource;

/** Polls a LiveData-backed result until it leaves the LOADING state. */
public final class VmTestUtils {

    private VmTestUtils() {
    }

    public static <T> Resource<T> await(LiveData<Resource<T>> liveData, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        Resource<T> value = null;
        while (System.currentTimeMillis() < deadline) {
            value = liveData.getValue();
            if (value != null && value.status != Resource.Status.LOADING) {
                return value;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return value;
    }
}