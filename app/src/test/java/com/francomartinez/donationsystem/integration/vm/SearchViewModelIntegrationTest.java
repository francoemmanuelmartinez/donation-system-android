package com.francomartinez.donationsystem.integration.vm;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.francomartinez.donationsystem.integration.support.BaseApiTest;
import com.francomartinez.donationsystem.integration.support.TestDatabase;
import com.francomartinez.donationsystem.models.SearchResultItem;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.viewmodels.SearchViewModel;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SearchViewModelIntegrationTest extends BaseApiTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void searchByTextReturnsMatchingDonors() throws Exception {
        int aptoId = TestDatabase.insertDonor("55555555", "Juan", "Sosa", "O+", null);
        TestDatabase.insertDonorPhone(aptoId, "3515555555", true);
        TestDatabase.insertDonor("66666666", "Pedro", "Rios", "O+", null);

        SearchViewModel vm = new SearchViewModel();
        vm.search("Sosa", "", "");

        Resource<List<SearchResultItem>> result = VmTestUtils.await(vm.getSearchResult(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertEquals(1, result.data.size());
        assertEquals("Sosa", result.data.get(0).getLastName());
        assertEquals("Juan", result.data.get(0).getFirstName());
    }

    @Test
    public void searchReturnsDonorsInVeda() throws Exception {
        int vedaId = TestDatabase.insertDonor("66666666", "Pedro", "Rios", "O+", null);
        // Veda por recuperación post-donación: next_eligible_date futuro (el estado se calcula en SQL).
        TestDatabase.execute("UPDATE donors SET next_eligible_date = DATE_ADD(CURDATE(), INTERVAL 30 DAY) "
                + "WHERE id = " + vedaId);

        SearchViewModel vm = new SearchViewModel();
        vm.search("66666666", "", "");

        Resource<List<SearchResultItem>> result = VmTestUtils.await(vm.getSearchResult(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertEquals(1, result.data.size());
        assertFalse("Los donantes en veda no deben quedar excluidos", "APTO".equals(result.data.get(0).getState()));
    }

    @Test
    public void searchEmptyQueryReturnsAllDonors() throws Exception {
        TestDatabase.insertDonor("55555555", "Juan", "Sosa", "O+", null);
        TestDatabase.insertDonor("66666666", "Pedro", "Rios", "O+", null);

        SearchViewModel vm = new SearchViewModel();
        vm.search("", "", "");

        Resource<List<SearchResultItem>> result = VmTestUtils.await(vm.getSearchResult(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertEquals(2, result.data.size());
    }
}
