package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.SearchPage;
import com.francomartinez.donationsystem.models.SearchResultItem;
import com.francomartinez.donationsystem.repositories.SearchRepository;
import com.francomartinez.donationsystem.utils.Resource;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchViewModel extends ViewModel {

    public static final int PAGE_SIZE = 20;

    private final SearchRepository searchRepository;
    private final MutableLiveData<Resource<List<SearchResultItem>>> searchResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);

    private final List<SearchResultItem> accumulated = new ArrayList<>();
    private String q = "";
    private String group = "";
    private String factor = "";
    private int offset = 0;
    private boolean hasMore = false;
    private boolean inFlight = false;
    private int generation = 0;

    public SearchViewModel() {
        this.searchRepository = new SearchRepository();
    }

    public LiveData<Resource<List<SearchResultItem>>> getSearchResult() {
        return searchResult;
    }

    public LiveData<Boolean> getLoadingMore() {
        return loadingMore;
    }

    public boolean hasMore() {
        return hasMore;
    }

    /** Runs a fresh search (page 1) and resets accumulated results. */
    public void search(String q, String group, String factor) {
        this.q = safe(q);
        this.group = safe(group);
        this.factor = safe(factor);
        accumulated.clear();
        offset = 0;
        hasMore = false;
        loadingMore.postValue(false);
        inFlight = true;
        int gen = ++generation;
        searchResult.postValue(Resource.loading());
        fetch(gen);
    }

    /** Appends the next page when available. */
    public void loadNextPage() {
        if (inFlight || !hasMore) {
            return;
        }
        inFlight = true;
        loadingMore.postValue(true);
        fetch(generation);
    }

    private void fetch(int gen) {
        searchRepository.search(q, group, factor, PAGE_SIZE, offset,
                new Callback<ApiResponse<SearchPage>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<SearchPage>> call, Response<ApiResponse<SearchPage>> response) {
                        if (gen != generation) {
                            return;
                        }
                        inFlight = false;
                        loadingMore.postValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<SearchPage> body = response.body();
                            if (body.isSuccess()) {
                                SearchPage page = body.getData();
                                List<SearchResultItem> items = page != null ? page.getItems() : new ArrayList<>();
                                if (offset == 0) {
                                    accumulated.clear();
                                }
                                accumulated.addAll(items);
                                offset += items.size();
                                hasMore = page != null && page.hasMore();
                                searchResult.postValue(Resource.success(new ArrayList<>(accumulated)));
                            } else {
                                searchResult.postValue(Resource.error(
                                        body.getMessage() != null ? body.getMessage() : "No se pudo realizar la búsqueda"));
                            }
                        } else {
                            searchResult.postValue(Resource.error("No se pudo realizar la búsqueda: " + response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<SearchPage>> call, Throwable t) {
                        if (gen != generation) {
                            return;
                        }
                        inFlight = false;
                        loadingMore.postValue(false);
                        searchResult.postValue(Resource.error("Error de red: " + t.getMessage()));
                    }
                });
    }

    private static String safe(String value) {
        return value != null ? value.trim() : "";
    }
}
