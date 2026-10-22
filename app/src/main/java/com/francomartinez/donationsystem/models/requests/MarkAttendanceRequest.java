package com.francomartinez.donationsystem.models.requests;

import com.google.gson.annotations.SerializedName;

public class MarkAttendanceRequest {

    @SerializedName("appointmentId")
    private final int appointmentId;

    @SerializedName("donorId")
    private final int donorId;

    @SerializedName("status")
    private final String status;

    @SerializedName("userId")
    private final Integer userId;

    public MarkAttendanceRequest(int appointmentId, int donorId, String status, Integer userId) {
        this.appointmentId = appointmentId;
        this.donorId = donorId;
        this.status = status;
        this.userId = userId;
    }
}
