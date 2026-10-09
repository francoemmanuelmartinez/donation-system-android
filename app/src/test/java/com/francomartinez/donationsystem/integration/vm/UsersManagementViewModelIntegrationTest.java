package com.francomartinez.donationsystem.integration.vm;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.francomartinez.donationsystem.integration.support.BaseApiTest;
import com.francomartinez.donationsystem.integration.support.TestDatabase;
import com.francomartinez.donationsystem.models.User;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.viewmodels.UsersManagementViewModel;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UsersManagementViewModelIntegrationTest extends BaseApiTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void loadPendingUsersSucceeds() throws Exception {
        int pendingId = TestDatabase.insertPendingUser("Ana", "Perez", "11111111", "ana@test.local");

        UsersManagementViewModel vm = new UsersManagementViewModel();
        vm.loadPendingUsers();

        Resource<List<User>> result = VmTestUtils.await(vm.getPendingUsers(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertTrue(result.data.stream().anyMatch(u -> u.getId() == pendingId && !u.isApproved()));
    }

    @Test
    public void loadActiveUsersContainsSeededDirector() throws Exception {
        UsersManagementViewModel vm = new UsersManagementViewModel();
        vm.loadActiveUsers();

        Resource<List<User>> result = VmTestUtils.await(vm.getActiveUsers(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertTrue(result.data.stream().anyMatch(u -> u.getId() == 1 && "DIRECTORA".equals(u.getRole())));
    }

    @Test
    public void approveUserSucceeds() throws Exception {
        int pendingId = TestDatabase.insertPendingUser("Ana", "Perez", "11111111", "ana@test.local");

        UsersManagementViewModel vm = new UsersManagementViewModel();
        vm.approveUser(new User(pendingId, "Ana", "Perez", "11111111", "ana@test.local", "NONE", false),
                "ADMINISTRATIVA", 1);

        Resource<String> result = VmTestUtils.await(vm.getOperationMessage(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertEquals(1, TestDatabase.queryInt("SELECT is_approved FROM users WHERE id = " + pendingId));
    }

    @Test
    public void updateRoleCannotDegradeSoleDirector() throws Exception {
        int secondDirector = TestDatabase.insertApprovedUser("Luis", "Gomez", "22222222", "luis@test.local", "DIRECTORA");

        UsersManagementViewModel vm = new UsersManagementViewModel();
        // Two directors: allowed.
        vm.updateUserRole(secondDirector, "ADMINISTRATIVA");
        Resource<String> first = VmTestUtils.await(vm.getOperationMessage(), 5000);
        assertEquals(Resource.Status.SUCCESS, first.status);

        // Only director left: mutex must trigger a 403 error surfaced by the VM.
        vm.updateUserRole(1, "ADMINISTRATIVA");
        Resource<String> second = VmTestUtils.await(vm.getOperationMessage(), 5000);
        assertEquals(Resource.Status.ERROR, second.status);
        assertTrue(second.message.contains("Directora"));
    }
}