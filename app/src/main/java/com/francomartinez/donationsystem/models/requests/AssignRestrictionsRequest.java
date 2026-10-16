package com.francomartinez.donationsystem.models.requests;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AssignRestrictionsRequest {

    @SerializedName("donorId")
    private int donorId;

    @SerializedName("createdByUserId")
    private Integer createdByUserId;

    @SerializedName("restrictions")
    private List<AssignRestrictionRequest> restrictions;

    @SerializedName("reschedule")
    private RescheduleRequest reschedule;

    public AssignRestrictionsRequest() {
    }

    public AssignRestrictionsRequest(int donorId, Integer createdByUserId, List<AssignRestrictionRequest> restrictions, RescheduleRequest reschedule) {
        this.donorId = donorId;
        this.createdByUserId = createdByUserId;
        this.restrictions = restrictions;
        this.reschedule = reschedule;
    }

    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
    }

    public Integer getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Integer createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public List<AssignRestrictionRequest> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(List<AssignRestrictionRequest> restrictions) {
        this.restrictions = restrictions;
    }

    public RescheduleRequest getReschedule() {
        return reschedule;
    }

    public void setReschedule(RescheduleRequest reschedule) {
        this.reschedule = reschedule;
    }
}
