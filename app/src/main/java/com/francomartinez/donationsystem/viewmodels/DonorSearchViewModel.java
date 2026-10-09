package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.SearchResultItem;
import com.francomartinez.donationsystem.repositories.DonorRepository;
import com.francomartinez.donationsystem.utils.Resource;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DonorSearchViewModel extends ViewModel {

    private final DonorRepository donorRepository;
    private final MutableLiveData<Resource<List<SearchResultItem>>> searchResult = new MutableLiveData<>();

    public DonorSearchViewModel() {
        this.donorRepository = new DonorRepository();
    }

    public LiveData<Resource<List<SearchResultItem>>> getSearchResult() {
        return searchResult;
    }

    public void searchByDni(String dni) {
        if (dni == null || dni.trim().isEmpty()) {
            searchResult.postValue(Resource.success(new ArrayList<>()));
            return;
        }

        searchResult.postValue(Resource.loading());
        donorRepository.searchDonorsByDni(dni.trim(), new Callback<ApiResponse<List<SearchResultItem>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<SearchResultItem>>> call, Response<ApiResponse<List<SearchResultItem>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<SearchResultItem>> body = response.body();
                    if (body.isSuccess()) {
                        searchResult.postValue(Resource.success(body.getData()));
                    } else {
                        searchResult.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudo realizar la búsqueda"));
                    }
                } else {
                    searchResult.postValue(Resource.error("No se pudo realizar la búsqueda: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<SearchResultItem>>> call, Throwable t) {
                searchResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }
}