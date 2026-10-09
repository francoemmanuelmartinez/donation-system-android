package com.francomartinez.donationsystem.repositories;

import com.francomartinez.donationsystem.api.ApiService;
import com.francomartinez.donationsystem.api.RetrofitClient;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.Appointment;
import com.francomartinez.donationsystem.models.requests.CreateAppointmentRequest;
import com.francomartinez.donationsystem.models.requests.ReminderRequest;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;

public class AppointmentRepository {

    private final ApiService apiService;

    public AppointmentRepository() {
        apiService = RetrofitClient.getApiService();
    }

    public void getAppointments(String weekDate, Callback<ApiResponse<List<Appointment>>> callback) {
        Call<ApiResponse<List<Appointment>>> call = apiService.getAppointments(weekDate);
        call.enqueue(callback);
    }

    public void getAppointmentsByMonth(String month, Callback<ApiResponse<List<Appointment>>> callback) {
        Call<ApiResponse<List<Appointment>>> call = apiService.getAppointmentsByMonth(month);
        call.enqueue(callback);
    }

    public void sendReminder(ReminderRequest request, Callback<ApiResponse<Map<String, Object>>> callback) {
        Call<ApiResponse<Map<String, Object>>> call = apiService.sendWhatsAppReminder(request);
        call.enqueue(callback);
    }

    public void createAppointment(CreateAppointmentRequest request, Callback<ApiResponse<Map<String, Object>>> callback) {
        Call<ApiResponse<Map<String, Object>>> call = apiService.createAppointment(request);
        call.enqueue(callback);
    }
}