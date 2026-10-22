package com.francomartinez.donationsystem.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Donation {

    @SerializedName("id")
    private int id;

    @SerializedName("donorId")
    private int donorId;

    @SerializedName("weight")
    private double weight;

    @SerializedName("height")
    private double height;

    @SerializedName("hto")
    private double hto;

    @SerializedName("plateletsCount")
    private int plateletsCount;

    @SerializedName("staffUserId")
    private Integer staffUserId;

    @SerializedName("procedures")
    private List<String> procedures;

    @SerializedName("appointmentId")
    private Integer appointmentId;

    public Donation() {
    }

    public Donation(int donorId, double weight, double height, double hto, int plateletsCount, Integer staffUserId, List<String> procedures) {
        this.donorId = donorId;
        this.weight = weight;
        this.height = height;
        this.hto = hto;
        this.plateletsCount = plateletsCount;
        this.staffUserId = staffUserId;
        this.procedures = procedures;
    }

    public Integer getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Integer appointmentId) {
        this.appointmentId = appointmentId;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getHto() {
        return hto;
    }

    public void setHto(double hto) {
        this.hto = hto;
    }

    public int getPlateletsCount() {
        return plateletsCount;
    }

    public void setPlateletsCount(int plateletsCount) {
        this.plateletsCount = plateletsCount;
    }

    public Integer getStaffUserId() {
        return staffUserId;
    }

    public void setStaffUserId(Integer staffUserId) {
        this.staffUserId = staffUserId;
    }

    public List<String> getProcedures() {
        return procedures;
    }

    public void setProcedures(List<String> procedures) {
        this.procedures = procedures;
    }
}
