package com.francomartinez.donationsystem.utils;

public final class RoleMapper {

    public static final String ROLE_ADMINISTRATIVA = "ADMINISTRATIVA";
    public static final String ROLE_DIRECTORA = "DIRECTORA";
    public static final String ROLE_SUPERADMIN = "SUPERADMIN";

    private RoleMapper() {
    }

    public static String toSpanish(String role) {
        if (role == null) {
            return "";
        }
        switch (role) {
            case ROLE_SUPERADMIN:
                return "Superadmin";
            case ROLE_DIRECTORA:
                return "Directora";
            case ROLE_ADMINISTRATIVA:
                return "Administrativa";
            default:
                return role;
        }
    }

    public static String fromSpanish(String label) {
        if (label == null) {
            return ROLE_ADMINISTRATIVA;
        }
        if (label.equals("Directora")) return ROLE_DIRECTORA;
        if (label.equals("Superadmin")) return ROLE_SUPERADMIN;
        return ROLE_ADMINISTRATIVA;
    }
}