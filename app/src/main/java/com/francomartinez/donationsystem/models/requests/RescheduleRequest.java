package com.francomartinez.donationsystem.models.requests;

import com.google.gson.annotations.SerializedName;

public class RescheduleRequest {

    @SerializedName("cancelAppointmentId")
    private Integer cancelAppointmentId;

    @SerializedName("appointmentDate")
    private String appointmentDate;

    @SerializedName("appointmentTime")
    private String appointmentTime;

    @SerializedName("targetProcedure")
    private String targetProcedure;

    public RescheduleRequest() {
    }

    public RescheduleRequest(Integer cancelAppointmentId, String appointmentDate, String appointmentTime, String targetProcedure) {
        this.cancelAppointmentId = cancelAppointmentId;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.targetProcedure = targetProcedure;
    }

    public Integer getCancelAppointmentId() {
        return cancelAppointmentId;
    }

    public void setCancelAppointmentId(Integer cancelAppointmentId) {
        this.cancelAppointmentId = cancelAppointmentId;
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
}
