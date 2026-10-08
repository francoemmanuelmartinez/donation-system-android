package com.francomartinez.donationsystem.models;

import com.google.gson.annotations.SerializedName;

public class ContactPhone {

    @SerializedName("phoneNumber")
    private String phoneNumber;

    @SerializedName("phoneType")
    private String phoneType;

    @SerializedName("isPrimary")
    private boolean isPrimary;

    public ContactPhone() {
    }

    public ContactPhone(String phoneNumber, String phoneType, boolean isPrimary) {
        this.phoneNumber = phoneNumber;
        this.phoneType = phoneType;
        this.isPrimary = isPrimary;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneType() {
        return phoneType;
    }

    public void setPhoneType(String phoneType) {
        this.phoneType = phoneType;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }
}
