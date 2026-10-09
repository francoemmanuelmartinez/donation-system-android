package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.SearchResultItem;
import com.francomartinez.donationsystem.repositories.SearchRepository;
import com.francomartinez.donationsystem.utils.Resource;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchViewModel extends ViewModel {

    private final SearchRepository searchRepository;
    private final MutableLiveData<Resource<List<SearchResultItem>>> searchResult = new MutableLiveData<>();

    public SearchViewModel() {
        this.searchRepository = new SearchRepository();
    }

    public LiveData<Resource<List<SearchResultItem>>> getSearchResult() {
        return searchResult;
    }

    public void searchByBloodType(String bloodType) {
        searchResult.postValue(Resource.loading());
        searchRepository.searchByBloodType(bloodType != null ? bloodType.trim() : "", new Callback<ApiResponse<List<SearchResultItem>>>() {
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