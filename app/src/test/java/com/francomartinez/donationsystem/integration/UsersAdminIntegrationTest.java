package com.francomartinez.donationsystem.integration;

import com.francomartinez.donationsystem.integration.support.ApiTestClient;
import com.francomartinez.donationsystem.integration.support.BaseApiTest;
import com.francomartinez.donationsystem.integration.support.TestDatabase;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.User;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UsersAdminIntegrationTest extends BaseApiTest {

    @Test
    public void pendingListContainsUnapprovedUsers() throws Exception {
        int pendingId = TestDatabase.insertPendingUser("Ana", "Perez", "11111111", "ana@test.local");

        ApiResponse<List<User>> response = ApiTestClient.call(ApiTestClient.api().getPendingUsers());
        assertTrue(response.isSuccess());
        assertTrue(response.getData().stream().anyMatch(u -> u.getId() == pendingId && !u.isApproved()));
    }

    @Test
    public void approveMovesUserToActiveList() throws Exception {
        int pendingId = TestDatabase.insertPendingUser("Ana", "Perez", "11111111", "ana@test.local");

        ApiResponse<Map<String, Object>> approval = ApiTestClient.call(ApiTestClient.api().approveUser(
                new com.francomartinez.donationsystem.models.requests.ApproveUserRequest(pendingId, "ADMINISTRATIVA", 1)));
        assertTrue(approval.isSuccess());

        assertFalse(ApiTestClient.call(ApiTestClient.api().getPendingUsers()).getData()
                .stream().anyMatch(u -> u.getId() == pendingId));

        ApiResponse<List<User>> active = ApiTestClient.call(ApiTestClient.api().getActiveUsers());
        assertTrue(active.getData().stream()
                .anyMatch(u -> u.getId() == pendingId && "ADMINISTRATIVA".equals(u.getRole()) && u.isApproved()));
    }

    @Test
    public void updateRoleCannotDegradeSoleDirector() throws Exception {
        int secondDirector = TestDatabase.insertApprovedUser("Luis", "Gomez", "22222222", "luis@test.local", "DIRECTORA");
        int directorId = 1;

        // Two active directors: degrading one is allowed.
        ApiResponse<Map<String, Object>> ok =
                ApiTestClient.call(ApiTestClient.api().updateUserRole(
                        new com.francomartinez.donationsystem.models.requests.ApproveUserRequest(secondDirector, "ADMINISTRATIVA", null)));
        assertTrue(ok.isSuccess());

        // Only one director left: degrading it must be rejected with 403 (mutex).
        assertEquals(403, ApiTestClient.status(ApiTestClient.api().updateUserRole(
                new com.francomartinez.donationsystem.models.requests.ApproveUserRequest(directorId, "ADMINISTRATIVA", null))));

        assertEquals("DIRECTORA", TestDatabase.queryString("SELECT role FROM users WHERE id = " + directorId));
    }

    @Test
    public void deleteRejectsSoleDirector() throws Exception {
        assertEquals(403, ApiTestClient.status(ApiTestClient.api().deleteUser(
                java.util.Collections.singletonMap("userId", 1))));
    }

    @Test
    public void deleteActiveAdminSucceeds() throws Exception {
        int adminId = TestDatabase.insertApprovedUser("Ana", "Perez", "11111111", "ana@test.local", "ADMINISTRATIVA");

        ApiResponse<Map<String, Object>> response = ApiTestClient.call(ApiTestClient.api().deleteUser(
                java.util.Collections.singletonMap("userId", adminId)));
        assertTrue(response.isSuccess());

        assertEquals(0, TestDatabase.queryInt("SELECT COUNT(*) FROM users WHERE id = " + adminId));
    }

    @Test
    public void approveWithInvalidRoleRejected() throws Exception {
        int pendingId = TestDatabase.insertPendingUser("Ana", "Perez", "11111111", "ana@test.local");

        assertEquals(400, ApiTestClient.status(ApiTestClient.api().approveUser(
                new com.francomartinez.donationsystem.models.requests.ApproveUserRequest(pendingId, "ROOT", 1))));
    }
}