package com.francomartinez.donationsystem.api;

import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.Appointment;
import com.francomartinez.donationsystem.models.Donation;
import com.francomartinez.donationsystem.models.Donor;
import com.francomartinez.donationsystem.models.MedicalRestriction;
import com.francomartinez.donationsystem.models.RecoveryPolicy;
import com.francomartinez.donationsystem.models.SearchPage;
import com.francomartinez.donationsystem.models.Statistics;
import com.francomartinez.donationsystem.models.User;
import com.francomartinez.donationsystem.models.requests.ApproveUserRequest;
import com.francomartinez.donationsystem.models.requests.AssignRestrictionRequest;
import com.francomartinez.donationsystem.models.requests.AssignRestrictionsRequest;
import com.francomartinez.donationsystem.models.requests.CreateAppointmentRequest;
import com.francomartinez.donationsystem.models.requests.LoginRequest;
import com.francomartinez.donationsystem.models.requests.RegisterRequest;
import com.francomartinez.donationsystem.models.requests.ReminderRequest;
import com.francomartinez.donationsystem.models.requests.RevokeRestrictionRequest;
import com.francomartinez.donationsystem.models.requests.UpdateRestrictionRequest;

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

    @POST("users_admin.php?action=update_role")
    Call<ApiResponse<Map<String, Object>>> updateUserRole(@Body ApproveUserRequest request);

    // --- Donors ---
    @GET("donors.php")
    Call<ApiResponse<Donor>> getDonorByDni(@Query("dni") String dni);

    @GET("donors.php")
    Call<ApiResponse<Donor>> getDonorById(@Query("id") int id);

    @POST("donors.php")
    Call<ApiResponse<Map<String, Object>>> saveDonor(@Body Donor donor);

    // --- Unified Donor Search ---
    @GET("search.php")
    Call<ApiResponse<SearchPage>> searchDonors(
            @Query("q") String q,
            @Query("group") String group,
            @Query("factor") String factor,
            @Query("limit") int limit,
            @Query("offset") int offset);

    // --- Donations ---
    @POST("donations.php")
    Call<ApiResponse<Map<String, Object>>> saveDonation(@Body Donation donation);

    // --- Appointments ---
    @GET("appointments.php")
    Call<ApiResponse<List<Appointment>>> getAppointments(@Query("week_date") String weekDate);

    @GET("appointments.php")
    Call<ApiResponse<List<Appointment>>> getAppointmentsByMonth(@Query("month") String month);

    @POST("appointments.php?action=send_reminder")
    Call<ApiResponse<Map<String, Object>>> sendWhatsAppReminder(@Body ReminderRequest request);

    @POST("appointments.php?action=dispatch_reminders")
    Call<ApiResponse<Map<String, Object>>> dispatchReminders(@Body Map<String, Object> body);

    @POST("appointments.php?action=create_appointment")
    Call<ApiResponse<Map<String, Object>>> createAppointment(@Body CreateAppointmentRequest request);

    // --- Medical Restrictions ---
    @GET("restrictions.php")
    Call<ApiResponse<List<MedicalRestriction>>> getRestrictionsDictionary();

    @POST("restrictions.php?action=create")
    Call<ApiResponse<MedicalRestriction>> createRestriction(@Body MedicalRestriction restriction);

    @POST("restrictions.php?action=assign")
    Call<ApiResponse<Map<String, Object>>> assignDonorRestriction(@Body AssignRestrictionRequest request);

    @POST("restrictions.php?action=assign")
    Call<ApiResponse<Map<String, Object>>> assignDonorRestrictions(@Body AssignRestrictionsRequest request);

    @POST("restrictions.php?action=update")
    Call<ApiResponse<Map<String, Object>>> updateDonorRestriction(@Body UpdateRestrictionRequest request);

    @POST("restrictions.php?action=revoke")
    Call<ApiResponse<Map<String, Object>>> revokeDonorRestriction(@Body RevokeRestrictionRequest request);

    @POST("restrictions.php?action=delete")
    Call<ApiResponse<Map<String, Object>>> deleteRestriction(@Body Map<String, Object> body);

    // --- Statistics ---
    @GET("statistics.php")
    Call<ApiResponse<Statistics>> getStatistics(@Query("range") String range);

    // --- Recovery Policies (Director ABM) ---
    @GET("recovery_policies.php")
    Call<ApiResponse<List<RecoveryPolicy>>> getRecoveryPolicies();

    @POST("recovery_policies.php?action=create")
    Call<ApiResponse<RecoveryPolicy>> createRecoveryPolicy(@Body RecoveryPolicy policy);

    @POST("recovery_policies.php?action=update")
    Call<ApiResponse<Map<String, Object>>> updateRecoveryPolicy(@Body RecoveryPolicy policy);

    @POST("recovery_policies.php?action=delete")
    Call<ApiResponse<Map<String, Object>>> deleteRecoveryPolicy(@Body Map<String, Object> body);
}
