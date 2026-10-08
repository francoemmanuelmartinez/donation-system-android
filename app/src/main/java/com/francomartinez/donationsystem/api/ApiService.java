package com.francomartinez.donationsystem.api;

import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.Appointment;
import com.francomartinez.donationsystem.models.Donation;
import com.francomartinez.donationsystem.models.Donor;
import com.francomartinez.donationsystem.models.MedicalRestriction;
import com.francomartinez.donationsystem.models.User;
import com.francomartinez.donationsystem.models.requests.ApproveUserRequest;
import com.francomartinez.donationsystem.models.requests.AssignRestrictionRequest;
import com.francomartinez.donationsystem.models.requests.LoginRequest;
import com.francomartinez.donationsystem.models.requests.RegisterRequest;
import com.francomartinez.donationsystem.models.requests.ReminderRequest;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // --- Authentication ---
    @POST("login.php")
    Call<ApiResponse<User>> login(@Body LoginRequest request);

    @POST("register.php")
    Call<ApiResponse<Map<String, Object>>> register(@Body RegisterRequest request);

    // --- Staff Management (Director Admin) ---
    @GET("users_admin.php?action=pending")
    Call<ApiResponse<List<User>>> getPendingUsers();

    @GET("users_admin.php?action=active")
    Call<ApiResponse<List<User>>> getActiveUsers();

    @POST("users_admin.php?action=approve")
    Call<ApiResponse<Map<String, Object>>> approveUser(@Body ApproveUserRequest request);

    @POST("users_admin.php?action=delete")
    Call<ApiResponse<Map<String, Object>>> deleteUser(@Body Map<String, Object> body);

    // --- Donors ---
    @GET("donors.php")
    Call<ApiResponse<Donor>> getDonorByDni(@Query("dni") String dni);

    @POST("donors.php")
    Call<ApiResponse<Map<String, Object>>> saveDonor(@Body Donor donor);

    // --- Donations ---
    @POST("donations.php")
    Call<ApiResponse<Map<String, Object>>> saveDonation(@Body Donation donation);

    // --- Appointments ---
    @GET("appointments.php")
    Call<ApiResponse<List<Appointment>>> getAppointments(@Query("week_date") String weekDate);

    @POST("appointments.php?action=send_reminder")
    Call<ApiResponse<Map<String, Object>>> sendWhatsAppReminder(@Body ReminderRequest request);

    // --- Medical Restrictions ---
    @GET("restrictions.php")
    Call<ApiResponse<List<MedicalRestriction>>> getRestrictionsDictionary();

    @POST("restrictions.php?action=create")
    Call<ApiResponse<MedicalRestriction>> createRestriction(@Body MedicalRestriction restriction);

    @POST("restrictions.php?action=assign")
    Call<ApiResponse<Map<String, Object>>> assignDonorRestriction(@Body AssignRestrictionRequest request);

    // --- Emergency Stock Search ---
    @GET("search.php")
    Call<ApiResponse<List<Donor>>> searchDonorsByBloodType(@Query("blood_type") String bloodType);
}
