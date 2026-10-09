package com.francomartinez.donationsystem.views.fragments;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.adapters.DonorCardAdapter;
import com.francomartinez.donationsystem.models.SearchResultItem;
import com.francomartinez.donationsystem.models.requests.CreateAppointmentRequest;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.viewmodels.AppointmentsViewModel;
import com.francomartinez.donationsystem.viewmodels.DonorSearchViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ScheduleAppointmentFragment extends Fragment {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());

    private EditText editTextDonorSearchDni;
    private MaterialButton buttonSearchDonorsByDni;
    private RecyclerView recyclerViewDonorSearch;
    private ScrollView scrollScheduleConfirm;
    private TextView textViewScheduleDonorName;
    private TextView textViewScheduleDonorDni;
    private TextView textViewScheduleDonorStatusBadge;
    private TextView textViewScheduleConfirmation;
    private MaterialButtonToggleGroup toggleGroupScheduleProcedure;
    private MaterialButton buttonConfirmAppointment;

    private DonorSearchViewModel searchViewModel;
    private AppointmentsViewModel appointmentsViewModel;
    private DonorCardAdapter adapter;

    private SearchResultItem selectedItem;
    private LocalDate selectedDate;
    private String selectedTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_appointment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editTextDonorSearchDni = view.findViewById(R.id.editTextDonorSearchDni);
        buttonSearchDonorsByDni = view.findViewById(R.id.buttonSearchDonorsByDni);
        recyclerViewDonorSearch = view.findViewById(R.id.recyclerViewDonorSearch);
        scrollScheduleConfirm = view.findViewById(R.id.scrollScheduleConfirm);
        textViewScheduleDonorName = view.findViewById(R.id.textViewScheduleDonorName);
        textViewScheduleDonorDni = view.findViewById(R.id.textViewScheduleDonorDni);
        textViewScheduleDonorStatusBadge = view.findViewById(R.id.textViewScheduleDonorStatusBadge);
        textViewScheduleConfirmation = view.findViewById(R.id.textViewScheduleConfirmation);
        toggleGroupScheduleProcedure = view.findViewById(R.id.toggleGroupScheduleProcedure);
        buttonConfirmAppointment = view.findViewById(R.id.buttonConfirmAppointment);

        searchViewModel = new ViewModelProvider(this).get(DonorSearchViewModel.class);
        appointmentsViewModel = new ViewModelProvider(this).get(AppointmentsViewModel.class);

        adapter = new DonorCardAdapter(item -> onDonorCardClick(item));
        recyclerViewDonorSearch.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewDonorSearch.setAdapter(adapter);

        buttonSearchDonorsByDni.setOnClickListener(v -> performSearch());

        editTextDonorSearchDni.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        buttonConfirmAppointment.setOnClickListener(v -> confirmAppointment());

        searchViewModel.getSearchResult().observe(getViewLifecycleOwner(), result -> onSearchResult(result));
        appointmentsViewModel.getCreateResult().observe(getViewLifecycleOwner(), result -> onCreateResult(result));
    }

    private void performSearch() {
        String dni = editTextDonorSearchDni.getText() != null
                ? editTextDonorSearchDni.getText().toString().trim()
                : "";
        if (dni.isEmpty()) {
            Toast.makeText(requireContext(), R.string.hint_search_dni, Toast.LENGTH_SHORT).show();
            return;
        }
        hideKeyboard();
        searchViewModel.searchByDni(dni);
    }

    private void onSearchResult(Resource<List<SearchResultItem>> result) {
        if (result == null) {
            return;
        }
        if (result.status == Resource.Status.SUCCESS) {
            List<SearchResultItem> items = result.data != null ? result.data : Collections.emptyList();
            String query = editTextDonorSearchDni.getText() != null
                    ? editTextDonorSearchDni.getText().toString()
                    : "";
            adapter.submitList(items, query);
            if (items.isEmpty()) {
                Toast.makeText(requireContext(), R.string.msg_donor_search_empty, Toast.LENGTH_SHORT).show();
            }
        } else if (result.status == Resource.Status.ERROR && result.message != null) {
            Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show();
        }
    }

    private void onDonorCardClick(SearchResultItem item) {
        selectedItem = item;
        showDatePicker();
    }

    private void showDatePicker() {
        CalendarConstraints.DateValidator validator =
                DateValidatorPointForward.from(MaterialDatePicker.todayInUtcMilliseconds());

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.schedule_date_picker_title))
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(new CalendarConstraints.Builder().setValidator(validator).build())
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            selectedDate = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
            if (isBeforeRecoveryEnd()) {
                showRecoveryWarningDialog();
            } else {
                showTimePicker();
            }
        });

        picker.show(getParentFragmentManager(), "appointmentDatePicker");
    }

    private boolean isBeforeRecoveryEnd() {
        String next = selectedItem != null ? selectedItem.getNextEligibleDate() : null;
        if (next == null || next.trim().isEmpty()) {
            return false;
        }
        String normalized = next.trim();
        if (normalized.length() > 10) {
            normalized = normalized.substring(0, 10);
        }
        try {
            return LocalDate.parse(normalized).isAfter(selectedDate);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void showRecoveryWarningDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_warning_title)
                .setMessage(getString(R.string.warning_schedule_not_elapsed, formatDisplayDate(nextEligibleRaw())))
                .setCancelable(false)
                .setPositiveButton(R.string.btn_schedule_anyway, (dialog, which) -> showTimePicker())
                .setNegativeButton(R.string.btn_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String nextEligibleRaw() {
        return selectedItem != null ? selectedItem.getNextEligibleDate() : null;
    }

    private String formatDisplayDate(String dbDate) {
        try {
            String normalized = dbDate.trim();
            if (normalized.length() > 10) {
                normalized = normalized.substring(0, 10);
            }
            return LocalDate.parse(normalized).format(DISPLAY_DATE);
        } catch (Exception e) {
            return dbDate;
        }
    }

    private void showTimePicker() {
        TimePickerDialog timePicker = new TimePickerDialog(requireContext(),
                (dialog, hourOfDay, minute) -> {
                    selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    showConfirmation();
                }, 8, 0, true);
        timePicker.setTitle(getString(R.string.schedule_time_picker_title));
        timePicker.show();
    }

    private void showConfirmation() {
        recyclerViewDonorSearch.setVisibility(View.GONE);
        scrollScheduleConfirm.setVisibility(View.VISIBLE);

        textViewScheduleDonorName.setText(selectedItem.getFullName());
        textViewScheduleDonorDni.setText(getString(R.string.hint_dni) + " " + selectedItem.getDni());

        boolean enVeda = "EN_VEDA".equals(selectedItem.getStatus());
        if (enVeda) {
            textViewScheduleDonorStatusBadge.setText(R.string.badge_veda);
            textViewScheduleDonorStatusBadge.setBackgroundResource(R.drawable.bg_badge_red);
        } else {
            textViewScheduleDonorStatusBadge.setText(R.string.badge_apto);
            textViewScheduleDonorStatusBadge.setBackgroundResource(R.drawable.bg_badge_green);
        }

        textViewScheduleConfirmation.setText(getString(R.string.msg_schedule_confirmation,
                selectedItem.getFullName(),
                selectedDate.format(DISPLAY_DATE),
                selectedTime));
    }

    private void confirmAppointment() {
        if (selectedItem == null || selectedDate == null || selectedTime == null) {
            return;
        }

        String procedure = getSelectedProcedure();
        if (procedure == null) {
            Toast.makeText(requireContext(), R.string.msg_select_procedure, Toast.LENGTH_LONG).show();
            return;
        }

        CreateAppointmentRequest request = new CreateAppointmentRequest(
                selectedItem.getId(),
                selectedDate.toString(),
                selectedTime,
                procedure);
        appointmentsViewModel.createAppointment(request);
    }

    private String getSelectedProcedure() {
        List<Integer> checked = toggleGroupScheduleProcedure.getCheckedButtonIds();
        if (checked.isEmpty()) {
            return null;
        }
        if (checked.contains(R.id.buttonSchedulePlasma)) {
            return "PLASMA";
        }
        if (checked.contains(R.id.buttonSchedulePlatelets)) {
            return "PLATELETS";
        }
        return "BLOOD";
    }

    private void onCreateResult(Resource<java.util.Map<String, Object>> result) {
        if (result == null) {
            return;
        }
        if (result.status == Resource.Status.LOADING) {
            buttonConfirmAppointment.setEnabled(false);
            return;
        }
        buttonConfirmAppointment.setEnabled(true);
        if (result.status == Resource.Status.SUCCESS) {
            Toast.makeText(requireContext(), R.string.msg_appointment_saved, Toast.LENGTH_LONG).show();
            getParentFragmentManager().popBackStack();
        } else if (result.status == Resource.Status.ERROR) {
            Toast.makeText(requireContext(),
                    result.message != null ? result.message : getString(R.string.msg_appointment_save_error),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void hideKeyboard() {
        if (getContext() == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && editTextDonorSearchDni.getWindowToken() != null) {
            imm.hideSoftInputFromWindow(editTextDonorSearchDni.getWindowToken(), 0);
        }
    }
}