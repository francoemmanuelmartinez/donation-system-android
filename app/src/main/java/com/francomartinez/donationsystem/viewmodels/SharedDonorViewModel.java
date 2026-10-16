package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.Appointment;
import com.francomartinez.donationsystem.models.Donor;

public class SharedDonorViewModel extends ViewModel {

    private final MutableLiveData<Donor> selectedDonor = new MutableLiveData<>();
    private final MutableLiveData<String> selectedProcedure = new MutableLiveData<>();
    private final MutableLiveData<Appointment> selectedAppointment = new MutableLiveData<>();

    public LiveData<Donor> getSelectedDonor() {
        return selectedDonor;
    }

    public void setSelectedDonor(Donor donor) {
        selectedDonor.setValue(donor);
    }

    public void clearSelectedDonor() {
        selectedDonor.setValue(null);
    }

    public LiveData<String> getSelectedProcedure() {
        return selectedProcedure;
    }

    public void setSelectedProcedure(String procedure) {
        selectedProcedure.setValue(procedure);
    }

    public String consumeSelectedProcedure() {
        String procedure = selectedProcedure.getValue();
        selectedProcedure.setValue(null);
        return procedure;
    }

    public LiveData<Appointment> getSelectedAppointment() {
        return selectedAppointment;
    }

    public void setSelectedAppointment(Appointment appointment) {
        selectedAppointment.setValue(appointment);
    }

    public Appointment consumeSelectedAppointment() {
        Appointment appointment = selectedAppointment.getValue();
        selectedAppointment.setValue(null);
        return appointment;
    }
}
