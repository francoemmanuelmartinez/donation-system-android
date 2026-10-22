package com.francomartinez.donationsystem.models.requests;

import com.google.gson.annotations.SerializedName;

public class CancelAppointmentRequest {

    @SerializedName("appointmentId")
    private final int appointmentId;

    @SerializedName("donorId")
    private final int donorId;

    @SerializedName("cancelReason")
    private final String cancelReason;

    @SerializedName("userId")
    private final Integer userId;

    public CancelAppointmentRequest(int appointmentId, int donorId, String cancelReason, Integer userId) {
        this.appointmentId = appointmentId;
        this.donorId = donorId;
        this.cancelReason = cancelReason;
        this.userId = userId;
    }
}
