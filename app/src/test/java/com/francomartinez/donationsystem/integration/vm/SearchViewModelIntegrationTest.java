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
import static org.junit.Assert.assertTrue;

public class SearchViewModelIntegrationTest extends BaseApiTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void searchReturnsOnlyAptoDonors() throws Exception {
        int aptoId = TestDatabase.insertDonor("55555555", "Juan", "Sosa", "O+", "APTO", null);
        TestDatabase.insertDonorPhone(aptoId, "3515555555", true);
        int vedaId = TestDatabase.insertDonor("66666666", "Pedro", "Rios", "O+", "APTO", null);
        TestDatabase.insertDonorPhone(vedaId, "3516666666", true);
        TestDatabase.execute("UPDATE donors SET status = 'EN_VEDA' WHERE id = " + vedaId);

        SearchViewModel vm = new SearchViewModel();
        vm.searchByBloodType("O+");

        Resource<List<SearchResultItem>> result = VmTestUtils.await(vm.getSearchResult(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertEquals(1, result.data.size());
        assertEquals("Juan", result.data.get(0).getFirstName());
    }

    @Test
    public void searchWithoutMatchesReturnsEmptyList() throws Exception {
        SearchViewModel vm = new SearchViewModel();
        vm.searchByBloodType("AB-");

        Resource<List<SearchResultItem>> result = VmTestUtils.await(vm.getSearchResult(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertTrue(result.data.isEmpty());
    }

    @Test
    public void searchEmptyQueryReturnsEmptyList() throws Exception {
        SearchViewModel vm = new SearchViewModel();
        vm.searchByBloodType("");

        Resource<List<SearchResultItem>> result = VmTestUtils.await(vm.getSearchResult(), 5000);
        assertEquals(Resource.Status.SUCCESS, result.status);
        assertTrue(result.data.isEmpty());
    }
}