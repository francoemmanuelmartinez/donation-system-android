package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.Donor;

public class SharedDonorViewModel extends ViewModel {

    private final MutableLiveData<Donor> selectedDonor = new MutableLiveData<>();

    public LiveData<Donor> getSelectedDonor() {
        return selectedDonor;
    }

    public void setSelectedDonor(Donor donor) {
        selectedDonor.setValue(donor);
    }

    public void clearSelectedDonor() {
        selectedDonor.setValue(null);
    }
}
