package com.francomartinez.donationsystem.integration.vm;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.francomartinez.donationsystem.integration.support.BaseApiTest;
import com.francomartinez.donationsystem.integration.support.TestDatabase;
import com.francomartinez.donationsystem.models.User;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.viewmodels.LoginViewModel;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LoginViewModelIntegrationTest extends BaseApiTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void loginSucceedsForApprovedDirector() throws Exception {
        LoginViewModel vm = new LoginViewModel();
        vm.login("director@test.local", "password");

        Resource<User> result = VmTestUtils.await(vm.getLoginResult(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertEquals("DIRECTORA", result.data.getRole());
        assertTrue(result.data.isApproved());
    }

    @Test
    public void loginPendingUserReturnsError() throws Exception {
        TestDatabase.insertPendingUser("Ana", "Perez", "11111111", "ana@test.local");

        LoginViewModel vm = new LoginViewModel();
        vm.login("ana@test.local", "password");

        Resource<User> result = VmTestUtils.await(vm.getLoginResult(), 5000);
        assertEquals(Resource.Status.ERROR, result.status);
        assertTrue(result.message.contains("pendiente"));
    }

    @Test
    public void loginInvalidCredentialsReturnsError() {
        LoginViewModel vm = new LoginViewModel();
        vm.login("director@test.local", "malpassword");

        Resource<User> result = VmTestUtils.await(vm.getLoginResult(), 5000);
        assertEquals(Resource.Status.ERROR, result.status);
    }

    @Test
    public void loginWithEmptyFieldsFailsFast() {
        LoginViewModel vm = new LoginViewModel();
        vm.login("", "");
        Resource<User> result = VmTestUtils.await(vm.getLoginResult(), 1000);
        assertEquals(Resource.Status.ERROR, result.status);
    }
}