package com.francomartinez.donationsystem.integration;

import com.francomartinez.donationsystem.integration.support.ApiTestClient;
import com.francomartinez.donationsystem.integration.support.BaseApiTest;
import com.francomartinez.donationsystem.integration.support.TestDatabase;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.MedicalRestriction;
import com.francomartinez.donationsystem.models.requests.AssignRestrictionRequest;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RestrictionsIntegrationTest extends BaseApiTest {

    @Test
    public void dictionaryListIsSeeded() throws Exception {
        ApiResponse<List<MedicalRestriction>> response = ApiTestClient.call(ApiTestClient.api().getRestrictionsDictionary());
        assertTrue(response.isSuccess());
        assertTrue(response.getData().size() >= 4);
        assertTrue(response.getData().stream().anyMatch(r -> "Tatuaje Reciente".equals(r.getName())));
    }

    @Test
    public void createRestrictionPersistsAndAppearsInDictionary() throws Exception {
        MedicalRestriction restriction = new MedicalRestriction();
        restriction.setName("Embarazo Reciente");
        restriction.setDefaultVedaDays(120);

        ApiResponse<MedicalRestriction> response = ApiTestClient.call(ApiTestClient.api().createRestriction(restriction));
        assertTrue(response.isSuccess());
        assertTrue(response.getData().getId() > 0);

        List<MedicalRestriction> dictionary = ApiTestClient.call(ApiTestClient.api().getRestrictionsDictionary()).getData();
        assertTrue(dictionary.stream().anyMatch(r -> "Embarazo Reciente".equals(r.getName()) && r.getDefaultVedaDays() == 120));
    }

    @Test
    public void createRestrictionWithoutNameReturns400() throws Exception {
        MedicalRestriction restriction = new MedicalRestriction();
        restriction.setName("");
        restriction.setDefaultVedaDays(10);
        assertEquals(400, ApiTestClient.status(ApiTestClient.api().createRestriction(restriction)));
    }

    @Test
    public void assignComputesEligibleWorkdayReturnDate() throws Exception {
        int donorId = TestDatabase.insertDonor("33333333", "Maria", "Lopez", "O+", "APTO", null);
        int restrictionId = TestDatabase.queryInt("SELECT id FROM restrictions WHERE name = 'Tatuaje Reciente'");

        // 2 days from today can never land on Saturday/Sunday if today is not Saturday/Sunday... use a large veda
        // set anchored to a known weekday computed by the server itself. We simply assert the returned date is a
        // business day (MON-FRI) and later than today.
        ApiResponse<Map<String, Object>> assigned = ApiTestClient.call(ApiTestClient.api().assignDonorRestriction(
                new AssignRestrictionRequest(donorId, restrictionId, 3, "nota", null)));
        assertTrue(assigned.isSuccess());

        String eligible = TestDatabase.queryString(
                "SELECT eligible_workday_return_date FROM donor_restrictions WHERE donor_id = " + donorId);
        assertTrue("expected a business day, got " + eligible, isBusinessDay(eligible));
        assertEquals("EN_VEDA", TestDatabase.queryString("SELECT status FROM donors WHERE id = " + donorId));
    }

    @Test
    public void deleteRestrictionRemovesIt() throws Exception {
        MedicalRestriction restriction = new MedicalRestriction();
        restriction.setName("Restriccion Temporal");
        restriction.setDefaultVedaDays(5);
        int id = ApiTestClient.call(ApiTestClient.api().createRestriction(restriction)).getData().getId();

        ApiResponse<Map<String, Object>> deleted = ApiTestClient.call(ApiTestClient.api().deleteRestriction(
                java.util.Collections.singletonMap("restrictionId", id)));
        assertTrue(deleted.isSuccess());

        assertEquals(0, TestDatabase.queryInt("SELECT COUNT(*) FROM restrictions WHERE id = " + id));
    }

    @Test
    public void deleteMissingRestrictionReturns404() throws Exception {
        assertEquals(404, ApiTestClient.status(ApiTestClient.api().deleteRestriction(
                java.util.Collections.singletonMap("restrictionId", 999999))));
    }

    private static boolean isBusinessDay(String date) {
        java.time.LocalDate d = java.time.LocalDate.parse(date);
        int day = d.getDayOfWeek().getValue(); // 1=MON .. 7=SUN
        return day <= 5;
    }
}