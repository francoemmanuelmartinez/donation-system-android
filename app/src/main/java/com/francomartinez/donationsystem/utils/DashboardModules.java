package com.francomartinez.donationsystem.utils;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.models.DashboardModule;
import com.francomartinez.donationsystem.views.MainActivity;
import com.francomartinez.donationsystem.views.RecoveryPoliciesActivity;
import com.francomartinez.donationsystem.views.RestrictionsActivity;
import com.francomartinez.donationsystem.views.StatisticsActivity;
import com.francomartinez.donationsystem.views.UsersActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Define qué módulos ve cada rol en el dashboard.
 *
 * <p>Reglas actuales:
 * <ul>
 *     <li>ADMINISTRATIVA: flujo de donación + restricciones médicas + políticas de recuperación.</li>
 *     <li>DIRECTORA: todo lo anterior + estadísticas + gestión de personal.</li>
 *     <li>SUPERADMIN: todo + gestión de centros.</li>
 * </ul>
 */
public final class DashboardModules {

    private DashboardModules() {
    }

    public static List<DashboardModule> forRole(String role) {
        List<DashboardModule> modules = new ArrayList<>();

        modules.add(DashboardModule.forActivity(
                R.string.dashboard_menu_donations, R.drawable.ic_workflow, MainActivity.class));

        if (canViewStatistics(role)) {
            modules.add(DashboardModule.forActivity(
                    R.string.dashboard_menu_statistics, R.drawable.ic_statistics, StatisticsActivity.class));
        }

        modules.add(DashboardModule.forActivity(
                R.string.dashboard_menu_restrictions, R.drawable.ic_restrictions, RestrictionsActivity.class));

        if (canManageUsers(role)) {
            modules.add(DashboardModule.forActivity(
                    R.string.dashboard_menu_users, R.drawable.ic_users, UsersActivity.class));
        }

        modules.add(DashboardModule.forActivity(
                R.string.dashboard_menu_recovery_policies, R.drawable.ic_recovery, RecoveryPoliciesActivity.class));

        if (canManageCenters(role)) {
            modules.add(DashboardModule.forAction(
                    R.string.dashboard_menu_centers, R.drawable.ic_centers, DashboardModule.ACTION_CENTERS));
        }

        return modules;
    }

    public static boolean canViewStatistics(String role) {
        return isDirectorOrAbove(role);
    }

    public static boolean canManageUsers(String role) {
        return isDirectorOrAbove(role);
    }

    public static boolean canManageCenters(String role) {
        return RoleMapper.ROLE_SUPERADMIN.equalsIgnoreCase(role);
    }

    private static boolean isDirectorOrAbove(String role) {
        return RoleMapper.ROLE_DIRECTORA.equalsIgnoreCase(role)
                || RoleMapper.ROLE_SUPERADMIN.equalsIgnoreCase(role);
    }
}
