package com.francomartinez.donationsystem.integration;

import com.francomartinez.donationsystem.integration.support.ApiTestClient;
import com.francomartinez.donationsystem.integration.support.BaseApiTest;
import com.francomartinez.donationsystem.integration.support.TestDatabase;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.Donation;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DonationsIntegrationTest extends BaseApiTest {

    @Test
    public void saveDonationPersistsVitalsProceduresAndLastDonationDate() throws Exception {
        int donorId = TestDatabase.insertDonor("33333333", "Maria", "Lopez", "O+", "APTO", null);

        Donation donation = new Donation(donorId, 70.0, 1.75, 42.0, 180000, 1, Arrays.asList("PLASMA", "PLATELETS"));
        ApiResponse<Map<String, Object>> response = ApiTestClient.call(ApiTestClient.api().saveDonation(donation));
        assertTrue(response.isSuccess());

        assertEquals(1, TestDatabase.queryInt("SELECT COUNT(*) FROM donations WHERE donor_id = " + donorId));
        assertEquals(180000, TestDatabase.queryInt("SELECT platelets_count FROM donations WHERE donor_id = " + donorId));
        assertEquals(2, TestDatabase.queryInt("SELECT COUNT(*) FROM donation_procedures WHERE donation_id = (SELECT id FROM donations WHERE donor_id = " + donorId + ")"));
        assertEquals(1, TestDatabase.queryInt("SELECT COUNT(*) FROM donation_procedures WHERE donation_id = (SELECT id FROM donations WHERE donor_id = " + donorId + ") AND procedure_type = 'PLASMA'"));
        assertEquals(1, TestDatabase.queryInt("SELECT COUNT(*) FROM donation_procedures WHERE donation_id = (SELECT id FROM donations WHERE donor_id = " + donorId + ") AND procedure_type = 'PLATELETS'"));

        // last_donation_date is updated server side to the donation date (today).
        assertEquals(0, TestDatabase.queryInt("SELECT DATEDIFF(last_donation_date, CURDATE()) FROM donors WHERE id = " + donorId));
    }

    @Test
    public void saveDonationWithoutRequiredDataReturns400() throws Exception {
        int donorId = TestDatabase.insertDonor("33333333", "Maria", "Lopez", "O+", "APTO", null);

        Donation invalid = new Donation(donorId, 0, 1.75, 0, 0, 1, Arrays.asList("PLASMA"));
        assertEquals(400, ApiTestClient.status(ApiTestClient.api().saveDonation(invalid)));
    }

    @Test
    public void invalidProcedureTypesAreIgnored() throws Exception {
        int donorId = TestDatabase.insertDonor("33333333", "Maria", "Lopez", "O+", "APTO", null);

        Donation donation = new Donation(donorId, 70.0, 1.75, 42.0, 180000, 1, Arrays.asList("PLASMA", "NOT_A_PROC"));
        ApiResponse<Map<String, Object>> response = ApiTestClient.call(ApiTestClient.api().saveDonation(donation));
        assertTrue(response.isSuccess());

        assertEquals(1, TestDatabase.queryInt("SELECT COUNT(*) FROM donation_procedures WHERE donation_id = (SELECT id FROM donations WHERE donor_id = " + donorId + ")"));
    }
}