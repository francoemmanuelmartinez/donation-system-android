package com.francomartinez.donationsystem.models;

import com.google.gson.annotations.SerializedName;

public class SearchResultItem {

    @SerializedName("id")
    private int id;

    @SerializedName("dni")
    private String dni;

    @SerializedName("firstName")
    private String firstName;

    @SerializedName("lastName")
    private String lastName;

    @SerializedName("bloodType")
    private String bloodType;

    @SerializedName("phenotype")
    private String phenotype;

    @SerializedName("status")
    private String status;

    @SerializedName("lastDonationDate")
    private String lastDonationDate;

    @SerializedName("phoneNumber")
    private String phoneNumber;

    @SerializedName("nextEligibleDate")
    private String nextEligibleDate;

    @SerializedName("state")
    private String state;

    @SerializedName("vedaEndDate")
    private String vedaEndDate;

    @SerializedName("vedaReason")
    private String vedaReason;

    @SerializedName("recoveryVedaEndDate")
    private String recoveryVedaEndDate;

    @SerializedName("restrictionVedaEndDate")
    private String restrictionVedaEndDate;

    @SerializedName("restrictionVedaReason")
    private String restrictionVedaReason;

    @SerializedName("nextAppointmentDate")
    private String nextAppointmentDate;

    @SerializedName("nextAppointmentProcedure")
    private String nextAppointmentProcedure;

    public SearchResultItem() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getPhenotype() {
        return phenotype;
    }

    public void setPhenotype(String phenotype) {
        this.phenotype = phenotype;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastDonationDate() {
        return lastDonationDate;
    }

    public void setLastDonationDate(String lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNextEligibleDate() {
        return nextEligibleDate;
    }

    public void setNextEligibleDate(String nextEligibleDate) {
        this.nextEligibleDate = nextEligibleDate;
    }

    public String getState() {
        return state != null ? state : status;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getVedaEndDate() {
        return vedaEndDate;
    }

    public void setVedaEndDate(String vedaEndDate) {
        this.vedaEndDate = vedaEndDate;
    }

    public String getVedaReason() {
        return vedaReason;
    }

    public void setVedaReason(String vedaReason) {
        this.vedaReason = vedaReason;
    }

    public String getRecoveryVedaEndDate() {
        return recoveryVedaEndDate;
    }

    public void setRecoveryVedaEndDate(String recoveryVedaEndDate) {
        this.recoveryVedaEndDate = recoveryVedaEndDate;
    }

    public String getRestrictionVedaEndDate() {
        return restrictionVedaEndDate;
    }

    public void setRestrictionVedaEndDate(String restrictionVedaEndDate) {
        this.restrictionVedaEndDate = restrictionVedaEndDate;
    }

    public String getRestrictionVedaReason() {
        return restrictionVedaReason;
    }

    public void setRestrictionVedaReason(String restrictionVedaReason) {
        this.restrictionVedaReason = restrictionVedaReason;
    }

    public String getNextAppointmentDate() {
        return nextAppointmentDate;
    }

    public void setNextAppointmentDate(String nextAppointmentDate) {
        this.nextAppointmentDate = nextAppointmentDate;
    }

    public String getNextAppointmentProcedure() {
        return nextAppointmentProcedure;
    }

    public void setNextAppointmentProcedure(String nextAppointmentProcedure) {
        this.nextAppointmentProcedure = nextAppointmentProcedure;
    }

    public boolean hasUpcomingAppointment() {
        return nextAppointmentDate != null && !nextAppointmentDate.trim().isEmpty()
                && nextAppointmentProcedure != null && !nextAppointmentProcedure.trim().isEmpty();
    }

    public String getFullName() {
        return (safe(lastName) + ", " + safe(firstName)).trim();
    }

    public String getInitial() {
        if (firstName != null && !firstName.isEmpty()) {
            return firstName.substring(0, 1).toUpperCase();
        }
        return lastName != null && !lastName.isEmpty() ? lastName.substring(0, 1).toUpperCase() : "?";
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}