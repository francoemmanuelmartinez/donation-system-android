package com.francomartinez.donationsystem.integration.vm;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.francomartinez.donationsystem.integration.support.BaseApiTest;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.viewmodels.RegisterViewModel;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RegisterViewModelIntegrationTest extends BaseApiTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void registerSucceedsForNewUser() throws Exception {
        RegisterViewModel vm = new RegisterViewModel();
        vm.register("Ana", "Perez", "11111111", "ana@test.local", "3511111111", "secret123");

        Resource<String> result = VmTestUtils.await(vm.getRegisterResult(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertNotNull(result.data);
    }

    @Test
    public void registerDuplicateEmailReturnsError() throws Exception {
        RegisterViewModel first = new RegisterViewModel();
        first.register("Ana", "Perez", "11111111", "ana@test.local", "3511111111", "secret123");
        VmTestUtils.await(first.getRegisterResult(), 5000);

        RegisterViewModel duplicate = new RegisterViewModel();
        duplicate.register("Carlos", "Gomez", "22222222", "ana@test.local", "3512222222", "otrapass");

        Resource<String> result = VmTestUtils.await(duplicate.getRegisterResult(), 5000);
        assertEquals(Resource.Status.ERROR, result.status);
    }

    @Test
    public void registerWithEmptyFieldsFailsFast() {
        RegisterViewModel vm = new RegisterViewModel();
        vm.register("", "", "", "", "", "");
        Resource<String> result = VmTestUtils.await(vm.getRegisterResult(), 1000);
        assertEquals(Resource.Status.ERROR, result.status);
        assertNull(result.data);
    }
}