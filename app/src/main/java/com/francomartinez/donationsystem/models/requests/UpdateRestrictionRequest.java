package com.francomartinez.donationsystem.models.requests;

import com.google.gson.annotations.SerializedName;

public class UpdateRestrictionRequest {

    @SerializedName("donorRestrictionId")
    private int donorRestrictionId;

    @SerializedName("restrictionId")
    private int restrictionId;

    @SerializedName("vedaDays")
    private int vedaDays;

    @SerializedName("isPermanent")
    private boolean isPermanent;

    @SerializedName("notes")
    private String notes;

    @SerializedName("startDate")
    private String startDate;

    @SerializedName("reschedule")
    private RescheduleRequest reschedule;

    public UpdateRestrictionRequest() {
    }

    public UpdateRestrictionRequest(int donorRestrictionId, int restrictionId, int vedaDays, boolean isPermanent, String notes, String startDate) {
        this.donorRestrictionId = donorRestrictionId;
        this.restrictionId = restrictionId;
        this.vedaDays = vedaDays;
        this.isPermanent = isPermanent;
        this.notes = notes;
        this.startDate = startDate;
    }

    public int getDonorRestrictionId() {
        return donorRestrictionId;
    }

    public void setDonorRestrictionId(int donorRestrictionId) {
        this.donorRestrictionId = donorRestrictionId;
    }

    public int getRestrictionId() {
        return restrictionId;
    }

    public void setRestrictionId(int restrictionId) {
        this.restrictionId = restrictionId;
    }

    public int getVedaDays() {
        return vedaDays;
    }

    public void setVedaDays(int vedaDays) {
        this.vedaDays = vedaDays;
    }

    public boolean isPermanent() {
        return isPermanent;
    }

    public void setPermanent(boolean permanent) {
        isPermanent = permanent;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public RescheduleRequest getReschedule() {
        return reschedule;
    }

    public void setReschedule(RescheduleRequest reschedule) {
        this.reschedule = reschedule;
    }
}
