package com.francomartinez.donationsystem.models.requests;

import com.google.gson.annotations.SerializedName;

public class ReminderRequest {

    @SerializedName("appointmentId")
    private int appointmentId;

    @SerializedName("userId")
    private Integer userId;

    @SerializedName("channel")
    private String channel;

    public ReminderRequest() {
    }

    public ReminderRequest(int appointmentId, Integer userId) {
        this(appointmentId, userId, "whatsapp");
    }

    public ReminderRequest(int appointmentId, Integer userId, String channel) {
        this.appointmentId = appointmentId;
        this.userId = userId;
        this.channel = channel;
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

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
