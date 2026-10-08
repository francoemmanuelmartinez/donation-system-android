package com.francomartinez.donationsystem.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class User {

    @SerializedName("id")
    private int id;

    @SerializedName("firstName")
    private String firstName;

    @SerializedName("lastName")
    private String lastName;

    @SerializedName("dni")
    private String dni;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role; // "NONE", "ADMINISTRATIVA", "DIRECTORA"

    @SerializedName("isApproved")
    private boolean isApproved;

    @SerializedName("phones")
    private List<ContactPhone> phones;

    @SerializedName("emails")
    private List<ContactEmail> emails;

    public User() {
    }

    public User(int id, String firstName, String lastName, String dni, String email, String role, boolean isApproved) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dni = dni;
        this.email = email;
        this.role = role;
        this.isApproved = isApproved;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
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
}
