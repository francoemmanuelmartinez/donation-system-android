package com.francomartinez.donationsystem.integration.vm;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.francomartinez.donationsystem.integration.support.BaseApiTest;
import com.francomartinez.donationsystem.integration.support.TestDatabase;
import com.francomartinez.donationsystem.models.Donor;
import com.francomartinez.donationsystem.models.MedicalRestriction;
import com.francomartinez.donationsystem.models.requests.AssignRestrictionRequest;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.viewmodels.DonorsViewModel;

import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DonorsViewModelIntegrationTest extends BaseApiTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void getDonorByDniFound() throws Exception {
        int donorId = TestDatabase.insertDonor("33333333", "Maria", "Lopez", "O+", "APTO", null);
        TestDatabase.insertDonorPhone(donorId, "3514444444", true);

        DonorsViewModel vm = new DonorsViewModel();
        vm.getDonorByDni("33333333");

        Resource<Donor> result = VmTestUtils.await(vm.getDonorResult(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertEquals("Maria", result.data.getFirstName());
        assertEquals("APTO", result.data.getStatus());
    }

    @Test
    public void getDonorByDniNotFound() throws Exception {
        DonorsViewModel vm = new DonorsViewModel();
        vm.getDonorByDni("99999999");

        Resource<Donor> result = VmTestUtils.await(vm.getDonorResult(), 5000);
        assertEquals(Resource.Status.ERROR, result.status);
    }

    @Test
    public void saveDonorSucceeds() throws Exception {
        Donor donor = new Donor();
        donor.setDni("55555555");
        donor.setFirstName("Juan");
        donor.setLastName("Sosa");
        donor.setGender("Masculino");
        donor.setBirthDate("1990-01-01");
        donor.setCity("Cordoba");
        donor.setZipCode("5000");
        donor.setAddress("Calle 1");
        donor.setNeighborhood("Centro");
        donor.setBloodType("O+");
        donor.setPhenotype("");

        DonorsViewModel vm = new DonorsViewModel();
        vm.saveDonor(donor);

        Resource<Map<String, Object>> result = VmTestUtils.await(vm.getSaveResult(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
    }

    @Test
    public void loadRestrictionsDictionarySucceeds() throws Exception {
        DonorsViewModel vm = new DonorsViewModel();
        vm.loadRestrictions();

        Resource<List<MedicalRestriction>> result = VmTestUtils.await(vm.getRestrictionsResult(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertTrue(result.data.size() >= 4);
    }

    @Test
    public void assignRestrictionsMarksDonorEnVeda() throws Exception {
        int donorId = TestDatabase.insertDonor("88888888", "Ana", "Suarez", "B+", "APTO", null);
        int restrictionId = TestDatabase.queryInt("SELECT id FROM restrictions WHERE name = 'Tatuaje Reciente'");

        DonorsViewModel vm = new DonorsViewModel();
        vm.assignRestrictions(Collections.singletonList(
                new AssignRestrictionRequest(donorId, restrictionId, 60, "nota", null)));

        Resource<Map<String, Object>> result = VmTestUtils.await(vm.getAssignResult(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertEquals("EN_VEDA", TestDatabase.queryString("SELECT status FROM donors WHERE id = " + donorId));
    }
}