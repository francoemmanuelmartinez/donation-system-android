package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.Statistics;
import com.francomartinez.donationsystem.repositories.StatisticsRepository;
import com.francomartinez.donationsystem.utils.Resource;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatisticsViewModel extends ViewModel {

    private final StatisticsRepository repository;
    private final Map<String, MutableLiveData<Resource<Statistics>>> statisticsByRange = new HashMap<>();

    public StatisticsViewModel() {
        this.repository = new StatisticsRepository();
    }

    public LiveData<Resource<Statistics>> getStatistics(String range) {
        if (!statisticsByRange.containsKey(range)) {
            statisticsByRange.put(range, new MutableLiveData<>());
        }
        return statisticsByRange.get(range);
    }

    public void loadStatistics(String range) {
        MutableLiveData<Resource<Statistics>> current = statisticsByRange.get(range);
        if (current == null) {
            current = new MutableLiveData<>();
            statisticsByRange.put(range, current);
        }
        final MutableLiveData<Resource<Statistics>> liveData = current;
        liveData.postValue(Resource.loading());

        repository.getStatistics(range, new Callback<ApiResponse<Statistics>>() {
            @Override
            public void onResponse(Call<ApiResponse<Statistics>> call, Response<ApiResponse<Statistics>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Statistics> body = response.body();
                    if (body.isSuccess()) {
                        liveData.postValue(Resource.success(body.getData()));
                    } else {
                        liveData.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudieron obtener las estadísticas"));
                    }
                } else {
                    liveData.postValue(Resource.error("No se pudieron obtener las estadísticas: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Statistics>> call, Throwable t) {
                liveData.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }
}