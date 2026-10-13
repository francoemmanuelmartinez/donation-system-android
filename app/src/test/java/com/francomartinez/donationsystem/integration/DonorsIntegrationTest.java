package com.francomartinez.donationsystem.integration;

import com.francomartinez.donationsystem.integration.support.ApiTestClient;
import com.francomartinez.donationsystem.integration.support.BaseApiTest;
import com.francomartinez.donationsystem.integration.support.TestDatabase;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.ContactEmail;
import com.francomartinez.donationsystem.models.ContactPhone;
import com.francomartinez.donationsystem.models.Donor;
import com.francomartinez.donationsystem.models.SearchPage;
import com.francomartinez.donationsystem.models.requests.AssignRestrictionRequest;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DonorsIntegrationTest extends BaseApiTest {

    private static Donor buildDonor() {
        Donor donor = new Donor();
        donor.setDni("33333333");
        donor.setFirstName("Maria");
        donor.setLastName("Lopez");
        donor.setGender("Femenino");
        donor.setBirthDate("1985-05-20");
        donor.setCity("Cordoba");
        donor.setZipCode("5000");
        donor.setAddress("Calle Falsa 123");
        donor.setNeighborhood("Nueva Cordoba");
        donor.setBloodType("O+");
        donor.setPhenotype("CCD.ee");
        donor.setPhones(Collections.singletonList(new ContactPhone("3514444444", "Mobile", true)));
        donor.setEmails(Collections.singletonList(new ContactEmail("maria@test.local", "Personal", true)));
        return donor;
    }

    @Test
    public void createDonorThenFetchByDni() throws Exception {
        ApiResponse<Map<String, Object>> response = ApiTestClient.call(ApiTestClient.api().saveDonor(buildDonor()));
        assertTrue(response.isSuccess());

        ApiResponse<Donor> fetched = ApiTestClient.call(ApiTestClient.api().getDonorByDni("33333333"));
        assertTrue(fetched.isSuccess());
        Donor donor = fetched.getData();
        assertEquals("Maria", donor.getFirstName());
        assertEquals("Lopez", donor.getLastName());
        assertEquals("O+", donor.getBloodType());
        assertEquals("APTO", donor.getStatus());
        assertEquals(1, donor.getPhones().size());
        assertEquals(1, donor.getEmails().size());
    }

    @Test
    public void updateExistingDonorKeepsSameRow() throws Exception {
        ApiTestClient.call(ApiTestClient.api().saveDonor(buildDonor()));
        int originalId = TestDatabase.queryInt("SELECT id FROM donors WHERE dni = '33333333'");

        Donor updated = buildDonor();
        updated.setLastName("Rodriguez");
        ApiTestClient.call(ApiTestClient.api().saveDonor(updated));

        assertEquals(1, TestDatabase.queryInt("SELECT COUNT(*) FROM donors WHERE dni = '33333333'"));
        assertEquals(originalId, TestDatabase.queryInt("SELECT id FROM donors WHERE dni = '33333333'"));
        assertEquals("Rodriguez", TestDatabase.queryString("SELECT last_name FROM donors WHERE dni = '33333333'"));
    }

    @Test
    public void getDonorByDniNotFoundReturns404() throws Exception {
        assertEquals(404, ApiTestClient.status(ApiTestClient.api().getDonorByDni("99999999")));
    }

    @Test
    public void savingDonorWithoutRequiredFieldsReturns400() throws Exception {
        Donor incomplete = new Donor();
        incomplete.setDni("44444444");
        incomplete.setFirstName("SoloNombre");
        incomplete.setBloodType("A+");
        assertEquals(400, ApiTestClient.status(ApiTestClient.api().saveDonor(incomplete)));
    }

    @Test
    public void searchByTextAndGroupReturnsMatchingDonors() throws Exception {
        int aptoId = TestDatabase.insertDonor("55555555", "Juan", "Sosa", "O+", "APTO", null);
        TestDatabase.insertDonorPhone(aptoId, "3515555555", true);
        int vedaId = TestDatabase.insertDonor("66666666", "Pedro", "Rios", "O+", "APTO", null);
        TestDatabase.insertDonorPhone(vedaId, "3516666666", true);
        // Distinto grupo: no debe aparecer al filtrar por "O".
        TestDatabase.insertDonor("77777777", "Luis", "Toro", "A-", "APTO", null);

        // Marca a Pedro en veda por recuperación (estado calculado en SQL a partir de next_eligible_date).
        TestDatabase.execute("UPDATE donors SET next_eligible_date = DATE_ADD(CURDATE(), INTERVAL 30 DAY) "
                + "WHERE id = " + vedaId);

        ApiResponse<SearchPage> byGroup = ApiTestClient.call(
                ApiTestClient.api().searchDonors("", "O", "", 20, 0));
        assertTrue(byGroup.isSuccess());
        assertNotNull(byGroup.getData());
        assertEquals(2, byGroup.getData().getItems().size());

        ApiResponse<SearchPage> byText = ApiTestClient.call(
                ApiTestClient.api().searchDonors("Sosa", "", "", 20, 0));
        assertTrue(byText.isSuccess());
        assertNotNull(byText.getData());
        assertEquals(1, byText.getData().getItems().size());
        assertEquals("Juan", byText.getData().getItems().get(0).getFirstName());
    }

    @Test
    public void assignRestrictionMarksDonorEnVeda() throws Exception {
        int donorId = TestDatabase.insertDonor("88888888", "Ana", "Suarez", "B+", "APTO", null);
        int restrictionId = TestDatabase.queryInt("SELECT id FROM restrictions WHERE name = 'Tatuaje Reciente'");

        ApiResponse<Map<String, Object>> assigned = ApiTestClient.call(ApiTestClient.api().assignDonorRestriction(
                new AssignRestrictionRequest(donorId, restrictionId, 60, "Tatuaje", null)));
        assertTrue(assigned.isSuccess());

        assertEquals("EN_VEDA", TestDatabase.queryString("SELECT status FROM donors WHERE id = " + donorId));
        assertEquals(1, TestDatabase.queryInt("SELECT COUNT(*) FROM donor_restrictions WHERE donor_id = " + donorId));

        ApiResponse<Donor> fetched = ApiTestClient.call(ApiTestClient.api().getDonorByDni("88888888"));
        assertEquals("EN_VEDA", fetched.getData().getStatus());
        assertEquals(1, fetched.getData().getActiveRestrictions().size());
    }
}