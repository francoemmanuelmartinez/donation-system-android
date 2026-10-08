package com.francomartinez.donationsystem.models.requests;

import com.google.gson.annotations.SerializedName;

public class ApproveUserRequest {

    @SerializedName("userId")
    private int userId;

    @SerializedName("role")
    private String role;

    @SerializedName("approvedByUserId")
    private Integer approvedByUserId;

    public ApproveUserRequest() {
    }

    public ApproveUserRequest(int userId, String role, Integer approvedByUserId) {
        this.userId = userId;
        this.role = role;
        this.approvedByUserId = approvedByUserId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getApprovedByUserId() {
        return approvedByUserId;
    }

    public void setApprovedByUserId(Integer approvedByUserId) {
        this.approvedByUserId = approvedByUserId;
    }
}
