package com.francomartinez.donationsystem.integration;

import com.francomartinez.donationsystem.integration.support.ApiTestClient;
import com.francomartinez.donationsystem.integration.support.BaseApiTest;
import com.francomartinez.donationsystem.integration.support.TestDatabase;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.Statistics;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StatisticsIntegrationTest extends BaseApiTest {

    private void seedData() throws Exception {
        // Donor created today with 3 donations today.
        int donorToday = TestDatabase.insertDonor("11111111", "Ana", "Perez", "O+", "APTO", null);
        TestDatabase.insertDonorPhone(donorToday, "3511111111", true);
        TestDatabase.insertDonation(donorToday, "PLASMA", null);
        TestDatabase.insertDonation(donorToday, "PLASMA", null);
        TestDatabase.insertDonation(donorToday, "PLATELETS", null);

        // Older donor + donation (two months ago) -> counted only in year range.
        int donorOld = TestDatabase.insertDonor("22222222", "Luis", "Gomez", "B-", "APTO",
                "DATE_SUB(NOW(), INTERVAL 2 MONTH)");
        TestDatabase.insertDonation(donorOld, "PLASMA", "DATE_SUB(NOW(), INTERVAL 2 MONTH)");

        // One active veda (eligible_workday_return_date >= today).
        int donorVeda = TestDatabase.insertDonor("33333333", "Maria", "Lopez", "A+", "APTO", null);
        TestDatabase.execute("INSERT INTO donor_restrictions (donor_id, restriction_id, start_date, veda_days, calculated_end_date, eligible_workday_return_date, notes) VALUES ("
                + donorVeda + ", 1, CURDATE(), 365, DATE_ADD(CURDATE(), INTERVAL 365 DAY), DATE_ADD(CURDATE(), INTERVAL 400 DAY), 'nota')");
    }

    @Test
    public void dayRangeReflectsTodayActivity() throws Exception {
        seedData();

        ApiResponse<Statistics> response = ApiTestClient.call(ApiTestClient.api().getStatistics("day"));
        assertTrue(response.isSuccess());
        Statistics stats = response.getData();
        assertEquals("day", stats.getRange());
        assertEquals(3, stats.getDonationsTotal());
        assertEquals(2, stats.getProcedureCount("PLASMA"));
        assertEquals(1, stats.getProcedureCount("PLATELETS"));
        // Two donors registered today (Ana + Maria); the 2-months-old donor is excluded from day range.
        assertEquals(2, stats.getDonorsRegistered());
        assertEquals(1, stats.getDonorsInVeda());
        assertNotNull(stats.getSeries());
    }

    @Test
    public void weekAndMonthCountTodayOnly() throws Exception {
        seedData();

        assertEquals(3, ApiTestClient.call(ApiTestClient.api().getStatistics("week")).getData().getDonationsTotal());
        assertEquals(3, ApiTestClient.call(ApiTestClient.api().getStatistics("month")).getData().getDonationsTotal());
    }

    @Test
    public void yearRangeIncludesOlderDonation() throws Exception {
        seedData();

        Statistics stats = ApiTestClient.call(ApiTestClient.api().getStatistics("year")).getData();
        assertEquals(4, stats.getDonationsTotal());
    }

    @Test
    public void invalidRangeReturns400() throws Exception {
        assertEquals(400, ApiTestClient.status(ApiTestClient.api().getStatistics("decade")));
    }
}