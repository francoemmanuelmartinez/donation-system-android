package com.francomartinez.donationsystem.models;

import com.google.gson.annotations.SerializedName;

public class MedicalRestriction {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("restrictionName")
    private String restrictionName;

    @SerializedName("defaultVedaDays")
    private int defaultVedaDays;

    @SerializedName("startDate")
    private String startDate;

    @SerializedName("vedaDays")
    private int vedaDays;

    @SerializedName("calculatedEndDate")
    private String calculatedEndDate;

    @SerializedName("eligibleWorkdayReturnDate")
    private String eligibleWorkdayReturnDate;

    @SerializedName("notes")
    private String notes;

    public MedicalRestriction() {
    }

    public MedicalRestriction(int id, String name, int defaultVedaDays) {
        this.id = id;
        this.name = name;
        this.defaultVedaDays = defaultVedaDays;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : restrictionName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRestrictionName() {
        return restrictionName;
    }

    public void setRestrictionName(String restrictionName) {
        this.restrictionName = restrictionName;
    }

    public int getDefaultVedaDays() {
        return defaultVedaDays;
    }

    public void setDefaultVedaDays(int defaultVedaDays) {
        this.defaultVedaDays = defaultVedaDays;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public int getVedaDays() {
        return vedaDays;
    }

    public void setVedaDays(int vedaDays) {
        this.vedaDays = vedaDays;
    }

    public String getCalculatedEndDate() {
        return calculatedEndDate;
    }

    public void setCalculatedEndDate(String calculatedEndDate) {
        this.calculatedEndDate = calculatedEndDate;
    }

    public String getEligibleWorkdayReturnDate() {
        return eligibleWorkdayReturnDate;
    }

    public void setEligibleWorkdayReturnDate(String eligibleWorkdayReturnDate) {
        this.eligibleWorkdayReturnDate = eligibleWorkdayReturnDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
