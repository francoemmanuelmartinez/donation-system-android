package com.francomartinez.donationsystem.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Donor {

    @SerializedName("id")
    private int id;

    @SerializedName("dni")
    private String dni;

    @SerializedName("firstName")
    private String firstName;

    @SerializedName("lastName")
    private String lastName;

    @SerializedName("gender")
    private String gender;

    @SerializedName("birthDate")
    private String birthDate;

    @SerializedName("city")
    private String city;

    @SerializedName("address")
    private String address;

    @SerializedName("bloodType")
    private String bloodType;

    @SerializedName("phenotype")
    private String phenotype;

    @SerializedName("weight")
    private Double weight;

    @SerializedName("height")
    private Double height;

    @SerializedName("status")
    private String status; // "APTO", "EN_VEDA"

    @SerializedName("lastDonationDate")
    private String lastDonationDate;

    @SerializedName("nextEligibleDate")
    private String nextEligibleDate;

    @SerializedName("annualCounts")
    private java.util.Map<String, AnnualCount> annualCounts;

    @SerializedName("phones")
    private List<ContactPhone> phones;

    @SerializedName("emails")
    private List<ContactEmail> emails;

    @SerializedName("activeRestrictions")
    private List<MedicalRestriction> activeRestrictions;

    public Donor() {
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
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

    public String getNextEligibleDate() {
        return nextEligibleDate;
    }

    public void setNextEligibleDate(String nextEligibleDate) {
        this.nextEligibleDate = nextEligibleDate;
    }

    public java.util.Map<String, AnnualCount> getAnnualCounts() {
        return annualCounts;
    }

    public void setAnnualCounts(java.util.Map<String, AnnualCount> annualCounts) {
        this.annualCounts = annualCounts;
    }

    public List<ContactPhone> getPhones() {
        return phones;
    }

    public void setPhones(List<ContactPhone> phones) {
        this.phones = phones;
    }

    public List<ContactEmail> getEmails() {
        return emails;
    }

    public void setEmails(List<ContactEmail> emails) {
        this.emails = emails;
    }

    public List<MedicalRestriction> getActiveRestrictions() {
        return activeRestrictions;
    }

    public void setActiveRestrictions(List<MedicalRestriction> activeRestrictions) {
        this.activeRestrictions = activeRestrictions;
    }
}
