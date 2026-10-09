package com.francomartinez.donationsystem.integration;

import com.francomartinez.donationsystem.integration.support.ApiTestClient;
import com.francomartinez.donationsystem.integration.support.BaseApiTest;
import com.francomartinez.donationsystem.integration.support.TestDatabase;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.User;
import com.francomartinez.donationsystem.models.requests.ApproveUserRequest;
import com.francomartinez.donationsystem.models.requests.LoginRequest;
import com.francomartinez.donationsystem.models.requests.RegisterRequest;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AuthFlowIntegrationTest extends BaseApiTest {

    private static final String DNI = "11111111";

    @Test
    public void registerPersistsPendingUserWithContacts() throws Exception {
        ApiResponse<Map<String, Object>> response = ApiTestClient.call(ApiTestClient.api().register(
                new RegisterRequest("Ana", "Perez", DNI, "ana@test.local", "3511111111", "secret123")));
        assertTrue(response.isSuccess());

        assertEquals("NONE", TestDatabase.queryString("SELECT role FROM users WHERE dni = '" + DNI + "'"));
        assertEquals(0, TestDatabase.queryInt("SELECT is_approved FROM users WHERE dni = '" + DNI + "'"));
        assertEquals(1, TestDatabase.queryInt("SELECT COUNT(*) FROM user_phones WHERE user_id = (SELECT id FROM users WHERE dni = '" + DNI + "')"));
        assertEquals(1, TestDatabase.queryInt("SELECT COUNT(*) FROM user_emails WHERE user_id = (SELECT id FROM users WHERE dni = '" + DNI + "')"));
    }

    @Test
    public void loginRejectsPendingUserWith403() throws Exception {
        ApiTestClient.call(ApiTestClient.api().register(
                new RegisterRequest("Ana", "Perez", DNI, "ana@test.local", "3511111111", "secret123")));

        assertEquals(403, ApiTestClient.status(ApiTestClient.api().login(
                new LoginRequest("ana@test.local", "secret123"))));
    }

    @Test
    public void loginFailsWithInvalidCredentials() throws Exception {
        assertEquals(401, ApiTestClient.status(ApiTestClient.api().login(
                new LoginRequest("director@test.local", "wrong"))));
    }

    @Test
    public void approveThenLoginSucceedsWithRoleAndContacts() throws Exception {
        ApiTestClient.call(ApiTestClient.api().register(
                new RegisterRequest("Ana", "Perez", DNI, "ana@test.local", "3511111111", "secret123")));
        int userId = TestDatabase.userIdByDni(DNI);

        ApiResponse<Map<String, Object>> approval = ApiTestClient.call(ApiTestClient.api().approveUser(
                new ApproveUserRequest(userId, "DIRECTORA", 1)));
        assertTrue(approval.isSuccess());

        ApiResponse<User> login = ApiTestClient.call(ApiTestClient.api().login(
                new LoginRequest("ana@test.local", "secret123")));
        assertTrue(login.isSuccess());
        assertNotNull(login.getData());
        assertEquals("DIRECTORA", login.getData().getRole());
        assertTrue(login.getData().isApproved());
        assertEquals(1, login.getData().getPhones().size());
        assertEquals("3511111111", login.getData().getPhones().get(0).getPhoneNumber());
        assertTrue(login.getData().getPhones().get(0).isPrimary());
        assertEquals(1, login.getData().getEmails().size());
        assertEquals("ana@test.local", login.getData().getEmails().get(0).getEmailAddress());
        assertTrue(login.getData().getEmails().get(0).isPrimary());
    }

    @Test
    public void duplicateEmailAndDniRegistrationRejected() throws Exception {
        ApiTestClient.call(ApiTestClient.api().register(
                new RegisterRequest("Ana", "Perez", DNI, "ana@test.local", "3511111111", "secret123")));

        assertEquals(409, ApiTestClient.status(ApiTestClient.api().register(
                new RegisterRequest("Carlos", "Gomez", DNI, "carlos@test.local", "3512222222", "otrapass"))));
        assertEquals(409, ApiTestClient.status(ApiTestClient.api().register(
                new RegisterRequest("Carlos", "Gomez", "99999999", "ana@test.local", "3512222222", "otrapass"))));
    }

    @Test
    public void seededDirectorCanLogin() throws Exception {
        ApiResponse<User> login = ApiTestClient.call(ApiTestClient.api().login(
                new LoginRequest("director@test.local", "password")));
        assertTrue(login.isSuccess());
        assertEquals("DIRECTORA", login.getData().getRole());
        assertEquals(1, login.getData().getId());
    }
}