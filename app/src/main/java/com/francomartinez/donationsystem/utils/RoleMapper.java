package com.francomartinez.donationsystem.utils;

public final class RoleMapper {

    public static final String ROLE_ADMINISTRATIVA = "ADMINISTRATIVA";
    public static final String ROLE_DIRECTORA = "DIRECTORA";

    private RoleMapper() {
    }

    public static String toSpanish(String role) {
        if (role == null) {
            return "";
        }
        switch (role) {
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
        return label.equals("Directora") ? ROLE_DIRECTORA : ROLE_ADMINISTRATIVA;
    }
}