package com.francomartinez.donationsystem.models.requests;

import com.google.gson.annotations.SerializedName;

public class CreateAppointmentRequest {

    @SerializedName("donorId")
    private final int donorId;

    @SerializedName("appointmentDate")
    private final String appointmentDate;

    @SerializedName("appointmentTime")
    private final String appointmentTime;

    @SerializedName("targetProcedure")
    private final String targetProcedure;

    public CreateAppointmentRequest(int donorId, String appointmentDate, String appointmentTime, String targetProcedure) {
        this.donorId = donorId;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.targetProcedure = targetProcedure;
    }
}