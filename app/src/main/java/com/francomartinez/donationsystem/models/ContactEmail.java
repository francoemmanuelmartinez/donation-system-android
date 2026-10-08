package com.francomartinez.donationsystem.models;

import com.google.gson.annotations.SerializedName;

public class ContactEmail {

    @SerializedName("emailAddress")
    private String emailAddress;

    @SerializedName("emailType")
    private String emailType;

    @SerializedName("isPrimary")
    private boolean isPrimary;

    public ContactEmail() {
    }

    public ContactEmail(String emailAddress, String emailType, boolean isPrimary) {
        this.emailAddress = emailAddress;
        this.emailType = emailType;
        this.isPrimary = isPrimary;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getEmailType() {
        return emailType;
    }

    public void setEmailType(String emailType) {
        this.emailType = emailType;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }
}
