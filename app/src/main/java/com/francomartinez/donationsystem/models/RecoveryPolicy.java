package com.francomartinez.donationsystem.models;

import com.google.gson.annotations.SerializedName;

public class RecoveryPolicy {

    @SerializedName("id")
    private int id;

    @SerializedName("procedureType")
    private String procedureType;

    @SerializedName("recoveryDays")
    private int recoveryDays;

    @SerializedName("recoveryDaysFemale")
    private int recoveryDaysFemale;

    @SerializedName("maxPerYear")
    private int maxPerYear;

    @SerializedName("maxPerYearFemale")
    private int maxPerYearFemale;

    public RecoveryPolicy() {
    }

    public RecoveryPolicy(String procedureType, int recoveryDays, int recoveryDaysFemale, int maxPerYear, int maxPerYearFemale) {
        this.procedureType = procedureType;
        this.recoveryDays = recoveryDays;
        this.recoveryDaysFemale = recoveryDaysFemale;
        this.maxPerYear = maxPerYear;
        this.maxPerYearFemale = maxPerYearFemale;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProcedureType() {
        return procedureType;
    }

    public void setProcedureType(String procedureType) {
        this.procedureType = procedureType;
    }

    public int getRecoveryDays() {
        return recoveryDays;
    }

    public void setRecoveryDays(int recoveryDays) {
        this.recoveryDays = recoveryDays;
    }

    public int getRecoveryDaysFemale() {
        return recoveryDaysFemale;
    }

    public void setRecoveryDaysFemale(int recoveryDaysFemale) {
        this.recoveryDaysFemale = recoveryDaysFemale;
    }

    public int getMaxPerYear() {
        return maxPerYear;
    }

    public void setMaxPerYear(int maxPerYear) {
        this.maxPerYear = maxPerYear;
    }

    public int getMaxPerYearFemale() {
        return maxPerYearFemale;
    }

    public void setMaxPerYearFemale(int maxPerYearFemale) {
        this.maxPerYearFemale = maxPerYearFemale;
    }
}