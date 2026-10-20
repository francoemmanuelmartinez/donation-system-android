package com.francomartinez.donationsystem.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.francomartinez.donationsystem.models.User;

public class SessionManager {

    private static final String PREFS_NAME = "donation_session";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_ROLE = "role";
    private static final String KEY_FIRST_NAME = "firstName";
    private static final String KEY_LAST_NAME = "lastName";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_CENTER_ID = "centerId";

    private static SessionManager instance;

    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public void saveUser(User user) {
        if (user == null) return;
        prefs.edit()
                .putInt(KEY_USER_ID, user.getId())
                .putString(KEY_ROLE, user.getRole())
                .putString(KEY_FIRST_NAME, user.getFirstName())
                .putString(KEY_LAST_NAME, user.getLastName())
                .putString(KEY_EMAIL, user.getEmail())
                .apply();
        if (user.getCenterId() != null) {
            prefs.edit().putInt(KEY_CENTER_ID, user.getCenterId()).apply();
        } else {
            prefs.edit().remove(KEY_CENTER_ID).apply();
        }
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return getUserId() > 0;
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public Integer getUserIdOrNull() {
        int id = getUserId();
        return id > 0 ? id : null;
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public String getFirstName() {
        return prefs.getString(KEY_FIRST_NAME, "");
    }

    public String getFullName() {
        return (getFirstName() + " " + prefs.getString(KEY_LAST_NAME, "")).trim();
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public Integer getCenterId() {
        if (!prefs.contains(KEY_CENTER_ID)) return null;
        return prefs.getInt(KEY_CENTER_ID, -1);
    }
}
