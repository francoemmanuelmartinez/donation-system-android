package com.francomartinez.donationsystem.models.requests;

import com.google.gson.annotations.SerializedName;

public class AssignRestrictionRequest {

    @SerializedName("donorId")
    private int donorId;

    @SerializedName("restrictionId")
    private int restrictionId;

    @SerializedName("vedaDays")
    private int vedaDays;

    @SerializedName("notes")
    private String notes;

    @SerializedName("createdByUserId")
    private Integer createdByUserId;

    @SerializedName("isPermanent")
    private boolean isPermanent;

    public AssignRestrictionRequest() {
    }

    public AssignRestrictionRequest(int donorId, int restrictionId, int vedaDays, String notes, Integer createdByUserId) {
        this.donorId = donorId;
        this.restrictionId = restrictionId;
        this.vedaDays = vedaDays;
        this.notes = notes;
        this.createdByUserId = createdByUserId;
    }

    public AssignRestrictionRequest(int restrictionId, int vedaDays, String notes, boolean isPermanent) {
        this.restrictionId = restrictionId;
        this.vedaDays = vedaDays;
        this.notes = notes;
        this.isPermanent = isPermanent;
    }

    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Integer createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public boolean isPermanent() {
        return isPermanent;
    }

    public void setPermanent(boolean permanent) {
        isPermanent = permanent;
    }
}
