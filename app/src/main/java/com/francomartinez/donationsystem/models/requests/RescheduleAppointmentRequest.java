package com.francomartinez.donationsystem.models.requests;

import com.google.gson.annotations.SerializedName;

public class RescheduleAppointmentRequest {

    @SerializedName("appointmentId")
    private final int appointmentId;

    @SerializedName("donorId")
    private final int donorId;

    @SerializedName("appointmentDate")
    private final String appointmentDate;

    @SerializedName("appointmentTime")
    private final String appointmentTime;

    @SerializedName("targetProcedure")
    private final String targetProcedure;

    @SerializedName("userId")
    private final Integer userId;

    public RescheduleAppointmentRequest(int appointmentId, int donorId, String appointmentDate,
                                        String appointmentTime, String targetProcedure, Integer userId) {
        this.appointmentId = appointmentId;
        this.donorId = donorId;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.targetProcedure = targetProcedure;
        this.userId = userId;
    }
}
