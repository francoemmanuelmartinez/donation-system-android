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
import com.francomartinez.donationsystem.models.Donor;
import com.francomartinez.donationsystem.models.SearchResultItem;
import com.francomartinez.donationsystem.models.requests.CreateAppointmentRequest;
import com.francomartinez.donationsystem.models.requests.RescheduleAppointmentRequest;
import com.francomartinez.donationsystem.models.requests.UpdateAppointmentRequest;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.utils.SessionManager;
import com.francomartinez.donationsystem.viewmodels.AppointmentsViewModel;
import com.francomartinez.donationsystem.viewmodels.DonorsViewModel;
import com.francomartinez.donationsystem.viewmodels.SearchViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
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

    public static final String MODE_CREATE = "CREATE";
    public static final String MODE_EDIT = "EDIT";
    public static final String MODE_RESCHEDULE = "RESCHEDULE";

    private static final String ARG_MODE = "mode";
    private static final String ARG_DONOR_ID = "donorId";
    private static final String ARG_DONOR_NAME = "donorName";
    private static final String ARG_DONOR_DNI = "donorDni";
    private static final String ARG_APPOINTMENT_ID = "appointmentId";
    private static final String ARG_CURRENT_DATE = "currentDate";
    private static final String ARG_CURRENT_TIME = "currentTime";
    private static final String ARG_CURRENT_PROCEDURE = "currentProcedure";

    private EditText editTextDonorSearchDni;
    private View textInputLayoutDonorSearchDni;
    private MaterialButton buttonSearchDonorsByDni;
    private RecyclerView recyclerViewDonorSearch;
    private ScrollView scrollScheduleConfirm;
    private TextView textViewScheduleDonorName;
    private TextView textViewScheduleDonorDni;
    private Chip chipScheduleDonorStatus;
    private Chip chipScheduleUpcomingAppointment;
    private TextView textViewScheduleConfirmation;
    private MaterialButtonToggleGroup toggleGroupScheduleProcedure;
    private MaterialButton buttonConfirmAppointment;

    private SearchViewModel searchViewModel;
    private AppointmentsViewModel appointmentsViewModel;
    private DonorsViewModel donorsViewModel;
    private DonorCardAdapter adapter;

    private SearchResultItem selectedItem;
    private LocalDate selectedDate;
    private String selectedTime;

    private String mode = MODE_CREATE;
    private int argDonorId;
    private String argDonorName;
    private String argDonorDni;
    private int argAppointmentId;
    private String argCurrentDate;
    private String argCurrentTime;
    private String argCurrentProcedure;
    private String donorNextEligibleDate;

    public static ScheduleAppointmentFragment newCreateInstance() {
        return new ScheduleAppointmentFragment();
    }

    public static ScheduleAppointmentFragment newEditInstance(int appointmentId, int donorId, String donorName,
                                                              String donorDni, String date, String time, String procedure) {
        return newLifecycleInstance(MODE_EDIT, appointmentId, donorId, donorName, donorDni, date, time, procedure);
    }

    public static ScheduleAppointmentFragment newRescheduleInstance(int appointmentId, int donorId, String donorName,
                                                                    String donorDni, String date, String time, String procedure) {
        return newLifecycleInstance(MODE_RESCHEDULE, appointmentId, donorId, donorName, donorDni, date, time, procedure);
    }

    private static ScheduleAppointmentFragment newLifecycleInstance(String mode, int appointmentId, int donorId,
                                                                    String donorName, String donorDni,
                                                                    String date, String time, String procedure) {
        ScheduleAppointmentFragment fragment = new ScheduleAppointmentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode);
        args.putInt(ARG_APPOINTMENT_ID, appointmentId);
        args.putInt(ARG_DONOR_ID, donorId);
        args.putString(ARG_DONOR_NAME, donorName);
        args.putString(ARG_DONOR_DNI, donorDni);
        args.putString(ARG_CURRENT_DATE, date);
        args.putString(ARG_CURRENT_TIME, time);
        args.putString(ARG_CURRENT_PROCEDURE, procedure);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_appointment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        readArguments();

        editTextDonorSearchDni = view.findViewById(R.id.editTextDonorSearchDni);
        textInputLayoutDonorSearchDni = view.findViewById(R.id.textInputLayoutDonorSearchDni);
        buttonSearchDonorsByDni = view.findViewById(R.id.buttonSearchDonorsByDni);
        recyclerViewDonorSearch = view.findViewById(R.id.recyclerViewDonorSearch);
        scrollScheduleConfirm = view.findViewById(R.id.scrollScheduleConfirm);
        textViewScheduleDonorName = view.findViewById(R.id.textViewScheduleDonorName);
        textViewScheduleDonorDni = view.findViewById(R.id.textViewScheduleDonorDni);
        chipScheduleDonorStatus = view.findViewById(R.id.chipScheduleDonorStatus);
        chipScheduleUpcomingAppointment = view.findViewById(R.id.chipScheduleUpcomingAppointment);
        textViewScheduleConfirmation = view.findViewById(R.id.textViewScheduleConfirmation);
        toggleGroupScheduleProcedure = view.findViewById(R.id.toggleGroupScheduleProcedure);
        buttonConfirmAppointment = view.findViewById(R.id.buttonConfirmAppointment);

        searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        appointmentsViewModel = new ViewModelProvider(this).get(AppointmentsViewModel.class);
        donorsViewModel = new ViewModelProvider(this).get(DonorsViewModel.class);
        appointmentsViewModel.setCurrentUserId(SessionManager.getInstance(requireContext()).getUserIdOrNull());

        adapter = new DonorCardAdapter(DonorCardAdapter.Mode.COMPACT);
        adapter.setOnDonorClickListener(this::onDonorCardClick);
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
        appointmentsViewModel.getCreateResult().observe(getViewLifecycleOwner(), result -> onActionResult(result));
        appointmentsViewModel.getUpdateResult().observe(getViewLifecycleOwner(), result -> onActionResult(result));
        appointmentsViewModel.getRescheduleResult().observe(getViewLifecycleOwner(), result -> onActionResult(result));
        donorsViewModel.getDonorResult().observe(getViewLifecycleOwner(), result -> onDonorEligibilityResult(result));

        if (isLifecycleMode()) {
            setupLifecycleMode();
        }
    }

    private void readArguments() {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        String m = args.getString(ARG_MODE, MODE_CREATE);
        mode = m != null ? m : MODE_CREATE;
        argDonorId = args.getInt(ARG_DONOR_ID, 0);
        argDonorName = args.getString(ARG_DONOR_NAME);
        argDonorDni = args.getString(ARG_DONOR_DNI);
        argAppointmentId = args.getInt(ARG_APPOINTMENT_ID, 0);
        argCurrentDate = args.getString(ARG_CURRENT_DATE);
        argCurrentTime = args.getString(ARG_CURRENT_TIME);
        argCurrentProcedure = args.getString(ARG_CURRENT_PROCEDURE);
    }

    private boolean isLifecycleMode() {
        return MODE_EDIT.equals(mode) || MODE_RESCHEDULE.equals(mode);
    }

    private void setupLifecycleMode() {
        textInputLayoutDonorSearchDni.setVisibility(View.GONE);
        buttonSearchDonorsByDni.setVisibility(View.GONE);
        recyclerViewDonorSearch.setVisibility(View.GONE);

        buttonConfirmAppointment.setText(MODE_RESCHEDULE.equals(mode)
                ? R.string.btn_confirm_reschedule
                : R.string.btn_confirm_edit);

        // Precarga de la fecha/hora/procedimiento actuales y salto directo a la confirmación.
        selectedDate = parseDate(argCurrentDate);
        selectedTime = argCurrentTime;
        preselectProcedure(argCurrentProcedure);
        renderCurrentTurnCard();
        scrollScheduleConfirm.setVisibility(View.VISIBLE);

        // Elegibilidad del donante (avisa, no bloquea; si falla se permite sin aviso - D4).
        if (argDonorId > 0) {
            donorsViewModel.getDonorById(argDonorId);
        }
    }

    private void preselectProcedure(String procedure) {
        if (procedure == null) {
            return;
        }
        int buttonId;
        switch (procedure.trim().toUpperCase(Locale.ROOT)) {
            case "PLASMA":
                buttonId = R.id.buttonSchedulePlasma;
                break;
            case "PLATELETS":
                buttonId = R.id.buttonSchedulePlatelets;
                break;
            default:
                buttonId = R.id.buttonScheduleBlood;
                break;
        }
        toggleGroupScheduleProcedure.check(buttonId);
    }

    private void renderCurrentTurnCard() {
        textViewScheduleDonorName.setText(argDonorName != null ? argDonorName : "");
        textViewScheduleDonorDni.setText(getString(R.string.hint_dni) + " " + (argDonorDni != null ? argDonorDni : ""));
        chipScheduleDonorStatus.setVisibility(View.GONE);

        String dateText = selectedDate != null ? selectedDate.format(DISPLAY_DATE) : (argCurrentDate != null ? argCurrentDate : "");
        chipScheduleUpcomingAppointment.setVisibility(View.VISIBLE);
        chipScheduleUpcomingAppointment.setText(getString(R.string.label_current_appointment,
                procedureLabel(argCurrentProcedure), dateText, argCurrentTime != null ? argCurrentTime : ""));

        textViewScheduleConfirmation.setText(getString(R.string.msg_reschedule_confirmation,
                argDonorName != null ? argDonorName : ""));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim().substring(0, Math.min(10, value.trim().length())));
        } catch (Exception e) {
            return null;
        }
    }

    private void performSearch() {
        String query = editTextDonorSearchDni.getText() != null
                ? editTextDonorSearchDni.getText().toString().trim()
                : "";
        if (query.length() < 2) {
            Toast.makeText(requireContext(), R.string.msg_search_min_chars, Toast.LENGTH_SHORT).show();
            return;
        }
        hideKeyboard();
        searchViewModel.search(query, "", "");
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

    private void onDonorEligibilityResult(Resource<Donor> result) {
        if (result == null || result.status != Resource.Status.SUCCESS || result.data == null) {
            return; // D4: sin elegibilidad disponible, se permite operar igual.
        }
        Donor donor = result.data;
        donorNextEligibleDate = donor.getNextEligibleDate();

        boolean enVeda = "EN_VEDA".equals(donor.getStatus());
        chipScheduleDonorStatus.setVisibility(View.VISIBLE);
        if (enVeda) {
            chipScheduleDonorStatus.setText(R.string.badge_veda);
            chipScheduleDonorStatus.setChipBackgroundColorResource(R.color.veda_red);
        } else {
            chipScheduleDonorStatus.setText(R.string.badge_apto);
            chipScheduleDonorStatus.setChipBackgroundColorResource(R.color.aptitude_green);
        }
    }

    private boolean isBeforeRecoveryEnd() {
        String next = selectedItem != null ? selectedItem.getNextEligibleDate() : donorNextEligibleDate;
        if (next == null || next.trim().isEmpty() || selectedDate == null) {
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
        if (selectedItem != null) {
            return selectedItem.getNextEligibleDate();
        }
        return donorNextEligibleDate;
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

    private String procedureLabel(String procedure) {
        if (procedure == null) {
            return "";
        }
        switch (procedure.trim().toUpperCase(Locale.ROOT)) {
            case "PLASMA":
                return getString(R.string.procedure_plasma);
            case "PLATELETS":
                return getString(R.string.procedure_platelets);
            case "BLOOD":
                return getString(R.string.procedure_blood);
            default:
                return procedure;
        }
    }

    private void showTimePicker() {
        int initHour = 8;
        int initMinute = 0;
        if (argCurrentTime != null && argCurrentTime.matches("^\\d{1,2}:\\d{2}.*")) {
            try {
                String[] parts = argCurrentTime.split(":");
                initHour = Integer.parseInt(parts[0]);
                initMinute = Integer.parseInt(parts[1].substring(0, 2));
            } catch (Exception ignored) {
            }
        }
        TimePickerDialog timePicker = new TimePickerDialog(requireContext(),
                (dialog, hourOfDay, minute) -> {
                    selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    showConfirmation();
                }, initHour, initMinute, true);
        timePicker.setTitle(getString(R.string.schedule_time_picker_title));
        timePicker.show();
    }

    private void showConfirmation() {
        scrollScheduleConfirm.setVisibility(View.VISIBLE);

        if (isLifecycleMode()) {
            renderCurrentTurnCard();
            textViewScheduleConfirmation.setText(getString(R.string.msg_reschedule_confirmation,
                    argDonorName != null ? argDonorName : ""));
            return;
        }

        recyclerViewDonorSearch.setVisibility(View.GONE);
        if (textInputLayoutDonorSearchDni != null) {
            textInputLayoutDonorSearchDni.setVisibility(View.GONE);
        }
        if (buttonSearchDonorsByDni != null) {
            buttonSearchDonorsByDni.setVisibility(View.GONE);
        }

        textViewScheduleDonorName.setText(selectedItem.getFullName());
        textViewScheduleDonorDni.setText(getString(R.string.hint_dni) + " " + selectedItem.getDni());

        boolean enVeda = "EN_VEDA".equals(selectedItem.getStatus());
        if (enVeda) {
            chipScheduleDonorStatus.setText(R.string.badge_veda);
            chipScheduleDonorStatus.setChipBackgroundColorResource(R.color.veda_red);
        } else {
            chipScheduleDonorStatus.setText(R.string.badge_apto);
            chipScheduleDonorStatus.setChipBackgroundColorResource(R.color.aptitude_green);
        }
        chipScheduleDonorStatus.setVisibility(View.VISIBLE);

        if (selectedItem.hasUpcomingAppointment()) {
            chipScheduleUpcomingAppointment.setVisibility(View.VISIBLE);
            chipScheduleUpcomingAppointment.setText(getString(R.string.label_next_appointment,
                    procedureLabel(selectedItem.getNextAppointmentProcedure()),
                    formatDisplayDate(selectedItem.getNextAppointmentDate())));
        } else {
            chipScheduleUpcomingAppointment.setVisibility(View.GONE);
        }

        textViewScheduleConfirmation.setText(getString(R.string.msg_schedule_confirmation,
                selectedItem.getFullName(),
                selectedDate.format(DISPLAY_DATE),
                selectedTime));
    }

    private void confirmAppointment() {
        if (selectedDate == null || selectedTime == null) {
            return;
        }

        String procedure = getSelectedProcedure();
        if (procedure == null) {
            Toast.makeText(requireContext(), R.string.msg_select_procedure, Toast.LENGTH_LONG).show();
            return;
        }

        Integer userId = appointmentsViewModel.getCurrentUserId();

        if (MODE_RESCHEDULE.equals(mode)) {
            appointmentsViewModel.rescheduleAppointment(new RescheduleAppointmentRequest(
                    argAppointmentId, argDonorId, selectedDate.toString(), selectedTime, procedure, userId));
            return;
        }
        if (MODE_EDIT.equals(mode)) {
            appointmentsViewModel.updateAppointment(new UpdateAppointmentRequest(
                    argAppointmentId, argDonorId, selectedDate.toString(), selectedTime, procedure));
            return;
        }

        if (selectedItem == null) {
            return;
        }
        appointmentsViewModel.createAppointment(new CreateAppointmentRequest(
                selectedItem.getId(),
                selectedDate.toString(),
                selectedTime,
                procedure));
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

    private void onActionResult(Resource<java.util.Map<String, Object>> result) {
        if (result == null) {
            return;
        }
        if (result.status == Resource.Status.LOADING) {
            buttonConfirmAppointment.setEnabled(false);
            return;
        }
        buttonConfirmAppointment.setEnabled(true);
        if (result.status == Resource.Status.SUCCESS) {
            Toast.makeText(requireContext(), successMessage(), Toast.LENGTH_LONG).show();
            getParentFragmentManager().popBackStack();
        } else if (result.status == Resource.Status.ERROR) {
            Toast.makeText(requireContext(),
                    result.message != null ? result.message : getString(R.string.msg_appointment_save_error),
                    Toast.LENGTH_LONG).show();
        }
    }

    private int successMessage() {
        if (MODE_RESCHEDULE.equals(mode)) {
            return R.string.msg_appointment_rescheduled;
        }
        if (MODE_EDIT.equals(mode)) {
            return R.string.msg_appointment_updated;
        }
        return R.string.msg_appointment_saved;
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
