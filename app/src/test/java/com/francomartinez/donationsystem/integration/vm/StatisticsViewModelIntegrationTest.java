package com.francomartinez.donationsystem.integration.vm;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.francomartinez.donationsystem.integration.support.BaseApiTest;
import com.francomartinez.donationsystem.integration.support.TestDatabase;
import com.francomartinez.donationsystem.models.Statistics;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.viewmodels.StatisticsViewModel;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StatisticsViewModelIntegrationTest extends BaseApiTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void loadWeekStatisticsSucceeds() throws Exception {
        int donorId = TestDatabase.insertDonor("33333333", "Maria", "Lopez", "O+", null);
        TestDatabase.insertDonation(donorId, "PLASMA", null);
        TestDatabase.insertDonation(donorId, "PLASMA", null);
        TestDatabase.insertDonation(donorId, "PLATELETS", null);

        StatisticsViewModel vm = new StatisticsViewModel();
        vm.loadStatistics("week");

        Resource<Statistics> result = VmTestUtils.await(vm.getStatistics("week"), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertEquals("week", result.data.getRange());
        assertEquals(3, result.data.getDonationsTotal());
        assertEquals(2, result.data.getProcedureCount("PLASMA"));
        assertEquals(1, result.data.getProcedureCount("PLATELETS"));
        assertNotNull(result.data.getSeries());
    }

    @Test
    public void loadInvalidRangeReturnsError() throws Exception {
        StatisticsViewModel vm = new StatisticsViewModel();
        vm.loadStatistics("decade");

        Resource<Statistics> result = VmTestUtils.await(vm.getStatistics("decade"), 5000);
        assertEquals(Resource.Status.ERROR, result.status);
    }
}