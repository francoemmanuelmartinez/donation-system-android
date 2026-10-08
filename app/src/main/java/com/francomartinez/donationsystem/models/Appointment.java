package com.francomartinez.donationsystem.models;

import com.google.gson.annotations.SerializedName;

public class Appointment {

    @SerializedName("id")
    private int id;

    @SerializedName("donorId")
    private int donorId;

    @SerializedName("donorName")
    private String donorName;

    @SerializedName("donorPhone")
    private String donorPhone;

    @SerializedName("appointmentDate")
    private String appointmentDate;

    @SerializedName("appointmentTime")
    private String appointmentTime;

    @SerializedName("targetProcedure")
    private String targetProcedure;

    @SerializedName("status")
    private String status;

    @SerializedName("isReminderSent")
    private boolean isReminderSent;

    @SerializedName("reminderSentAt")
    private String reminderSentAt;

    public Appointment() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getDonorPhone() {
        return donorPhone;
    }

    public void setDonorPhone(String donorPhone) {
        this.donorPhone = donorPhone;
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(String appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(String appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getTargetProcedure() {
        return targetProcedure;
    }

    public void setTargetProcedure(String targetProcedure) {
        this.targetProcedure = targetProcedure;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isReminderSent() {
        return isReminderSent;
    }

    public void setReminderSent(boolean reminderSent) {
        isReminderSent = reminderSent;
    }

    public String getReminderSentAt() {
        return reminderSentAt;
    }

    public void setReminderSentAt(String reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }
}
