package com.francomartinez.donationsystem.models.requests;

import com.google.gson.annotations.SerializedName;

public class ReminderRequest {

    @SerializedName("appointmentId")
    private int appointmentId;

    @SerializedName("userId")
    private Integer userId;

    public ReminderRequest() {
    }

    public ReminderRequest(int appointmentId, Integer userId) {
        this.appointmentId = appointmentId;
        this.userId = userId;
    }

    public int getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(int appointmentId) {
        this.appointmentId = appointmentId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}
