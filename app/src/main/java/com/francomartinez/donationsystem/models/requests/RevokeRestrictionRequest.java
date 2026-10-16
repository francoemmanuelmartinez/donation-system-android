package com.francomartinez.donationsystem.models.requests;

import com.google.gson.annotations.SerializedName;

public class RevokeRestrictionRequest {

    @SerializedName("donorRestrictionId")
    private Integer donorRestrictionId;

    @SerializedName("donorId")
    private Integer donorId;

    public RevokeRestrictionRequest() {
    }

    public RevokeRestrictionRequest(Integer donorRestrictionId, Integer donorId) {
        this.donorRestrictionId = donorRestrictionId;
        this.donorId = donorId;
    }

    public Integer getDonorRestrictionId() {
        return donorRestrictionId;
    }

    public void setDonorRestrictionId(Integer donorRestrictionId) {
        this.donorRestrictionId = donorRestrictionId;
    }

    public Integer getDonorId() {
        return donorId;
    }

    public void setDonorId(Integer donorId) {
        this.donorId = donorId;
    }
}
