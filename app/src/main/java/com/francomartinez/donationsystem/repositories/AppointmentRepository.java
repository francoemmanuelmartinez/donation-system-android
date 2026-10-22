package com.francomartinez.donationsystem.repositories;

import com.francomartinez.donationsystem.api.ApiService;
import com.francomartinez.donationsystem.api.RetrofitClient;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.Appointment;
import com.francomartinez.donationsystem.models.requests.CancelAppointmentRequest;
import com.francomartinez.donationsystem.models.requests.CreateAppointmentRequest;
import com.francomartinez.donationsystem.models.requests.MarkAttendanceRequest;
import com.francomartinez.donationsystem.models.requests.ReminderRequest;
import com.francomartinez.donationsystem.models.requests.RescheduleAppointmentRequest;
import com.francomartinez.donationsystem.models.requests.UpdateAppointmentRequest;

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

    public void dispatchReminders(Callback<ApiResponse<Map<String, Object>>> callback) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("manual", "true");
        Call<ApiResponse<Map<String, Object>>> call = apiService.dispatchReminders(body);
        call.enqueue(callback);
    }

    public void createAppointment(CreateAppointmentRequest request, Callback<ApiResponse<Map<String, Object>>> callback) {
        Call<ApiResponse<Map<String, Object>>> call = apiService.createAppointment(request);
        call.enqueue(callback);
    }

    public void updateAppointment(UpdateAppointmentRequest request, Callback<ApiResponse<Map<String, Object>>> callback) {
        Call<ApiResponse<Map<String, Object>>> call = apiService.updateAppointment(request);
        call.enqueue(callback);
    }

    public void rescheduleAppointment(RescheduleAppointmentRequest request, Callback<ApiResponse<Map<String, Object>>> callback) {
        Call<ApiResponse<Map<String, Object>>> call = apiService.rescheduleAppointment(request);
        call.enqueue(callback);
    }

    public void cancelAppointment(CancelAppointmentRequest request, Callback<ApiResponse<Map<String, Object>>> callback) {
        Call<ApiResponse<Map<String, Object>>> call = apiService.cancelAppointment(request);
        call.enqueue(callback);
    }

    public void markAttendance(MarkAttendanceRequest request, Callback<ApiResponse<Map<String, Object>>> callback) {
        Call<ApiResponse<Map<String, Object>>> call = apiService.markAttendance(request);
        call.enqueue(callback);
    }

    public void getAppointmentsFiltered(String weekDate, String status, Callback<ApiResponse<List<Appointment>>> callback) {
        Call<ApiResponse<List<Appointment>>> call = apiService.getAppointmentsFiltered(weekDate, status);
        call.enqueue(callback);
    }

    public void getAppointmentsByMonthFiltered(String month, String status, Callback<ApiResponse<List<Appointment>>> callback) {
        Call<ApiResponse<List<Appointment>>> call = apiService.getAppointmentsByMonthFiltered(month, status);
        call.enqueue(callback);
    }
}