package com.francomartinez.donationsystem.views.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.adapters.AppointmentsAdapter;
import com.francomartinez.donationsystem.models.Appointment;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.viewmodels.AppointmentsViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppointmentHistoryFragment extends Fragment {

    private static final DateTimeFormatter DB_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());

    private static final int FILTER_ALL = 0;
    private static final int FILTER_ATTENDED = 1;
    private static final int FILTER_NO_SHOW = 2;
    private static final int FILTER_CANCELLED = 3;
    private static final int FILTER_RESCHEDULED = 4;

    private MaterialButton buttonHistoryFrom;
    private MaterialButton buttonHistoryTo;
    private Chip chipHistoryAll;
    private Chip chipHistoryAttended;
    private Chip chipHistoryNoShow;
    private Chip chipHistoryCancelled;
    private Chip chipHistoryRescheduled;
    private TextView textViewHistoryEmpty;
    private RecyclerView recyclerViewHistoryList;

    private AppointmentsViewModel viewModel;
    private AppointmentsAdapter appointmentsAdapter;

    private LocalDate fromDate;
    private LocalDate toDate;
    private final List<Appointment> allResolved = new ArrayList<>();
    private int typeFilter = FILTER_ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appointment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AppointmentsViewModel.class);

        buttonHistoryFrom = view.findViewById(R.id.buttonHistoryFrom);
        buttonHistoryTo = view.findViewById(R.id.buttonHistoryTo);
        chipHistoryAll = view.findViewById(R.id.chipHistoryAll);
        chipHistoryAttended = view.findViewById(R.id.chipHistoryAttended);
        chipHistoryNoShow = view.findViewById(R.id.chipHistoryNoShow);
        chipHistoryCancelled = view.findViewById(R.id.chipHistoryCancelled);
        chipHistoryRescheduled = view.findViewById(R.id.chipHistoryRescheduled);
        textViewHistoryEmpty = view.findViewById(R.id.textViewHistoryEmpty);
        recyclerViewHistoryList = view.findViewById(R.id.recyclerViewHistoryList);

        recyclerViewHistoryList.setLayoutManager(new LinearLayoutManager(requireContext()));
        appointmentsAdapter = new AppointmentsAdapter(null);
        appointmentsAdapter.setShowStatusChip(true);
        recyclerViewHistoryList.setAdapter(appointmentsAdapter);

        LocalDate today = LocalDate.now();
        fromDate = today.minusMonths(1);
        toDate = today;
        updateRangeLabels();

        buttonHistoryFrom.setOnClickListener(v -> pickDate(true));
        buttonHistoryTo.setOnClickListener(v -> pickDate(false));

        chipHistoryAll.setOnClickListener(v -> selectType(FILTER_ALL));
        chipHistoryAttended.setOnClickListener(v -> selectType(FILTER_ATTENDED));
        chipHistoryNoShow.setOnClickListener(v -> selectType(FILTER_NO_SHOW));
        chipHistoryCancelled.setOnClickListener(v -> selectType(FILTER_CANCELLED));
        chipHistoryRescheduled.setOnClickListener(v -> selectType(FILTER_RESCHEDULED));

        observeViewModel();
        loadHistory();
    }

    private void pickDate(boolean isFrom) {
        LocalDate initial = isFrom ? fromDate : toDate;
        new DatePickerDialog(requireContext(), (picker, year, month, dayOfMonth) -> {
            LocalDate picked = LocalDate.of(year, month + 1, dayOfMonth);
            if (isFrom) {
                fromDate = picked;
                if (fromDate.isAfter(toDate)) {
                    toDate = fromDate;
                }
            } else {
                toDate = picked;
                if (toDate.isBefore(fromDate)) {
                    fromDate = toDate;
                }
            }
            updateRangeLabels();
            loadHistory();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void updateRangeLabels() {
        buttonHistoryFrom.setText(getString(R.string.history_range_from, fromDate.format(DISPLAY_DATE)));
        buttonHistoryTo.setText(getString(R.string.history_range_to, toDate.format(DISPLAY_DATE)));
    }

    private void selectType(int type) {
        typeFilter = type;
        applyTypeFilter();
    }

    private void loadHistory() {
        viewModel.loadHistory(fromDate.format(DB_DATE), toDate.format(DB_DATE), "resolved");
    }

    private void observeViewModel() {
        viewModel.getHistoryResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS) {
                allResolved.clear();
                if (resource.data != null) {
                    allResolved.addAll(resource.data);
                }
                applyTypeFilter();
            } else if (resource.status == Resource.Status.ERROR) {
                String message = resource.message != null ? resource.message : getString(R.string.history_error_load);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyTypeFilter() {
        List<Appointment> filtered = new ArrayList<>();
        for (Appointment appointment : allResolved) {
            if (matchesType(appointment)) {
                filtered.add(appointment);
            }
        }
        appointmentsAdapter.submitList(filtered);
        textViewHistoryEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean matchesType(Appointment appointment) {
        String status = appointment.getStatus();
        if (status == null) {
            return false;
        }
        boolean rescheduled = "CANCELLED".equalsIgnoreCase(status)
                && appointment.getRescheduledToAppointmentId() != null
                && appointment.getRescheduledToAppointmentId() > 0;
        switch (typeFilter) {
            case FILTER_ATTENDED:
                return "ATTENDED".equalsIgnoreCase(status);
            case FILTER_NO_SHOW:
                return "NO_SHOW".equalsIgnoreCase(status);
            case FILTER_CANCELLED:
                return "CANCELLED".equalsIgnoreCase(status) && !rescheduled;
            case FILTER_RESCHEDULED:
                return rescheduled;
            case FILTER_ALL:
            default:
                return true;
        }
    }
}
