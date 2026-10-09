package com.francomartinez.donationsystem.integration.vm;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.francomartinez.donationsystem.integration.support.BaseApiTest;
import com.francomartinez.donationsystem.integration.support.TestDatabase;
import com.francomartinez.donationsystem.models.MedicalRestriction;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.viewmodels.RestrictionsManagementViewModel;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RestrictionsManagementViewModelIntegrationTest extends BaseApiTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void loadDictionarySucceeds() throws Exception {
        RestrictionsManagementViewModel vm = new RestrictionsManagementViewModel();
        vm.loadDictionary();

        Resource<List<MedicalRestriction>> result = VmTestUtils.await(vm.getDictionary(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertTrue(result.data.size() >= 4);
    }

    @Test
    public void createRestrictionSucceeds() throws Exception {
        RestrictionsManagementViewModel vm = new RestrictionsManagementViewModel();
        vm.createRestriction("Exclusion Test", 10);

        Resource<String> result = VmTestUtils.await(vm.getOperationMessage(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertEquals(1, TestDatabase.queryInt("SELECT COUNT(*) FROM restrictions WHERE name = 'Exclusion Test'"));
    }

    @Test
    public void deleteRestrictionSucceeds() throws Exception {
        TestDatabase.execute("INSERT INTO restrictions (name, default_veda_days) VALUES ('Exclusion Temp', 3)");
        int id = TestDatabase.queryInt("SELECT id FROM restrictions WHERE name = 'Exclusion Temp'");

        RestrictionsManagementViewModel vm = new RestrictionsManagementViewModel();
        vm.deleteRestriction(id);

        Resource<String> result = VmTestUtils.await(vm.getOperationMessage(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertEquals(0, TestDatabase.queryInt("SELECT COUNT(*) FROM restrictions WHERE id = " + id));
    }
}