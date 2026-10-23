package com.francomartinez.donationsystem.models;

/**
 * Módulo del dashboard. Puede navegar a una Activity o disparar una acción interna
 * (por ejemplo, el selector de centros del Superadmin).
 */
public class DashboardModule {

    public static final String ACTION_CENTERS = "centers";

    private final int titleRes;
    private final int iconRes;
    private final Class<?> targetActivity;
    private final String action;

    private DashboardModule(int titleRes, int iconRes, Class<?> targetActivity, String action) {
        this.titleRes = titleRes;
        this.iconRes = iconRes;
        this.targetActivity = targetActivity;
        this.action = action;
    }

    public static DashboardModule forActivity(int titleRes, int iconRes, Class<?> targetActivity) {
        return new DashboardModule(titleRes, iconRes, targetActivity, null);
    }

    public static DashboardModule forAction(int titleRes, int iconRes, String action) {
        return new DashboardModule(titleRes, iconRes, null, action);
    }

    public int getTitleRes() {
        return titleRes;
    }

    public int getIconRes() {
        return iconRes;
    }

    public Class<?> getTargetActivity() {
        return targetActivity;
    }

    public String getAction() {
        return action;
    }
}
