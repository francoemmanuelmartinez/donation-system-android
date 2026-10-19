package com.francomartinez.donationsystem.integration.support;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * JDBC helper that manages the dedicated integration test database
 * (donation_system_test). The schema fixture mirrors the production schema.
 */
public final class TestDatabase {

    public static final String DB_NAME = "donation_system_test";
    private static final String SERVER_URL = "jdbc:mysql://127.0.0.1:3306/?allowMultiQueries=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/" + DB_NAME
            + "?allowMultiQueries=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    /** Well-known BCrypt hash of "password" (used to seed approved staff accounts). */
    public static final String BCRYPT_PASSWORD = "$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private TestDatabase() {
    }

    public static Connection open() throws Exception {
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    /** Creates the database if needed and re-applies the whole fixture (drop + create + seed). */
    public static synchronized void resetSchema() throws Exception {
        try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             Statement st = c.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + DB_NAME
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }

        String schema = readResource("donation_system_test.sql");
        try (Connection c = open(); Statement st = c.createStatement()) {
            st.execute(schema);
        }
    }

    public static void execute(String sql) throws Exception {
        try (Connection c = open(); Statement st = c.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    public static int queryInt(String sql) throws Exception {
        try (Connection c = open();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public static String queryString(String sql) throws Exception {
        try (Connection c = open();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    // --- Seed helpers -------------------------------------------------------

    public static int insertApprovedUser(String firstName, String lastName, String dni, String email, String role)
            throws Exception {
        execute("INSERT INTO users (first_name, last_name, dni, email, password_hash, role, is_approved) VALUES ('"
                + esc(firstName) + "', '" + esc(lastName) + "', '" + esc(dni) + "', '" + esc(email) + "', '"
                + BCRYPT_PASSWORD + "', '" + role + "', 1)");
        return userIdByDni(dni);
    }

    public static int insertPendingUser(String firstName, String lastName, String dni, String email)
            throws Exception {
        execute("INSERT INTO users (first_name, last_name, dni, email, password_hash, role, is_approved) VALUES ('"
                + esc(firstName) + "', '" + esc(lastName) + "', '" + esc(dni) + "', '" + esc(email) + "', '"
                + BCRYPT_PASSWORD + "', 'NONE', 0)");
        return userIdByDni(dni);
    }

public static int insertDonor(String dni, String firstName, String lastName, String bloodType,
                                   String createdAt) throws Exception {
        execute("INSERT INTO donors (dni, first_name, last_name, gender, birth_date, city, address, blood_type, phenotype, last_donation_date, created_at) VALUES ('"
                + esc(dni) + "', '" + esc(firstName) + "', '" + esc(lastName) + "', 'Otro', '1990-01-01', 'Cordoba', 'Av Siempre 123', '"
                + esc(bloodType) + "', '', NULL, " + sqlDatetime(createdAt) + ")");
        return queryInt("SELECT id FROM donors WHERE dni = '" + esc(dni) + "'");
    }

    public static void insertDonorPhone(int donorId, String phoneNumber, boolean primary) throws Exception {
        execute("INSERT INTO donor_phones (donor_id, phone_number, phone_type, is_primary) VALUES (" + donorId
                + ", '" + esc(phoneNumber) + "', 'Mobile', " + (primary ? 1 : 0) + ")");
    }

    public static int insertDonation(int donorId, String procedure, String createdAt) throws Exception {
        execute("INSERT INTO donations (donor_id, weight, height, hto, platelets_count, staff_user_id, created_at) VALUES ("
                + donorId + ", 70.00, 1.75, 42.0, 180000, 1, " + sqlDatetime(createdAt) + ")");
        int donationId = queryInt("SELECT MAX(id) FROM donations WHERE donor_id = " + donorId);
        execute("INSERT INTO donation_procedures (donation_id, procedure_type) VALUES (" + donationId + ", '" + procedure + "')");
        return donationId;
    }

    public static int insertAppointment(int donorId, String appointmentDate, String time) throws Exception {
        execute("INSERT INTO appointments (donor_id, appointment_date, appointment_time, target_procedure, status) VALUES ("
                + donorId + ", '" + appointmentDate + "', '" + time + "', 'PLASMA', 'SCHEDULED')");
        return queryInt("SELECT MAX(id) FROM appointments WHERE donor_id = " + donorId);
    }

    public static int userIdByDni(String dni) throws Exception {
        return queryInt("SELECT id FROM users WHERE dni = '" + esc(dni) + "'");
    }

    private static String readResource(String name) throws Exception {
        try (InputStream in = TestDatabase.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource: " + name);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    private static String sqlDatetime(String value) {
        if (value == null) {
            return "NOW()";
        }
        return value.contains("(") ? value : "'" + esc(value) + "'";
    }

    private static String esc(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}