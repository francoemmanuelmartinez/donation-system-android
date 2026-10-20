package com.francomartinez.donationsystem.views.fragments;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.models.AnnualCount;
import com.francomartinez.donationsystem.models.Appointment;
import com.francomartinez.donationsystem.models.Donation;
import com.francomartinez.donationsystem.models.Donor;
import com.francomartinez.donationsystem.models.MedicalRestriction;
import com.francomartinez.donationsystem.models.requests.AssignRestrictionRequest;
import com.francomartinez.donationsystem.models.requests.AssignRestrictionsRequest;
import com.francomartinez.donationsystem.models.requests.RescheduleRequest;
import com.francomartinez.donationsystem.models.requests.RevokeRestrictionRequest;
import com.francomartinez.donationsystem.models.requests.UpdateRestrictionRequest;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.utils.SessionManager;
import com.francomartinez.donationsystem.viewmodels.DonationsViewModel;
import com.francomartinez.donationsystem.viewmodels.DonorsViewModel;
import com.francomartinez.donationsystem.viewmodels.SharedDonorViewModel;
import com.francomartinez.donationsystem.views.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DonationsFragment extends Fragment {

    private static final DateTimeFormatter DB_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());

    private TextView textViewDonorContextName;
    private TextView textViewDonorContextDni;
    private TextView textViewDonorContextBlood;
    private TextView textViewDonorContextStaff;
    private Chip chipDonorContextStatus;
    private MaterialButton buttonSeeDonorData;
    private TextInputEditText editTextHto;
    private TextInputEditText editTextPlateletsCount;
    private MaterialButtonToggleGroup toggleGroupDonationType;
    private MaterialButton buttonSaveDonation;
    private LinearLayout layoutDonationEntry;

    private TextView textViewNoActiveRestrictions;
    private LinearLayout layoutActiveRestrictions;
    private MaterialCheckBox checkApplyRestrictions;
    private LinearLayout layoutRestrictionsEditor;
    private AutoCompleteTextView spinnerAddRestriction;
    private ChipGroup chipGroupNewRestrictions;
    private MaterialCheckBox checkPermanentVeda;
    private TextInputLayout textInputLayoutVedaDays;
    private TextInputEditText editTextVedaDays;
    private TextInputEditText editTextVedaReturnDate;
    private TextInputEditText editTextRestrictionNotes;
    private MaterialCheckBox checkScheduleAppointment;
    private LinearLayout layoutAppointmentEditor;
    private TextInputEditText editTextAppointmentDate;
    private TextInputEditText editTextAppointmentTime;
    private AutoCompleteTextView spinnerAppointmentProcedure;

    private DonationsViewModel viewModel;
    private DonorsViewModel donorViewModel;
    private SharedDonorViewModel sharedDonorViewModel;
    private Donor selectedDonor;
    private Appointment selectedAppointment;

    private final List<MedicalRestriction> dictionaryRestrictions = new ArrayList<>();
    private final LinkedHashMap<String, Integer> newRestrictions = new LinkedHashMap<>();

    private boolean mutexUpdating = false;
    private boolean suppressVedaWatcher = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_donations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DonationsViewModel.class);
        donorViewModel = new ViewModelProvider(this).get(DonorsViewModel.class);
        sharedDonorViewModel = new ViewModelProvider(requireActivity()).get(SharedDonorViewModel.class);

        observeDonorRefresh();

        textViewDonorContextName = view.findViewById(R.id.textViewDonorContextName);
        textViewDonorContextDni = view.findViewById(R.id.textViewDonorContextDni);
        textViewDonorContextBlood = view.findViewById(R.id.textViewDonorContextBlood);
        textViewDonorContextStaff = view.findViewById(R.id.textViewDonorContextStaff);
        chipDonorContextStatus = view.findViewById(R.id.chipDonorContextStatus);
        buttonSeeDonorData = view.findViewById(R.id.buttonSeeDonorData);
        editTextHto = view.findViewById(R.id.editTextHto);
        editTextPlateletsCount = view.findViewById(R.id.editTextPlateletsCount);
        toggleGroupDonationType = view.findViewById(R.id.toggleGroupDonationType);
        buttonSaveDonation = view.findViewById(R.id.buttonSaveDonation);
        layoutDonationEntry = view.findViewById(R.id.layoutDonationEntry);

        textViewNoActiveRestrictions = view.findViewById(R.id.textViewNoActiveRestrictions);
        layoutActiveRestrictions = view.findViewById(R.id.layoutActiveRestrictions);
        checkApplyRestrictions = view.findViewById(R.id.checkApplyRestrictions);
        layoutRestrictionsEditor = view.findViewById(R.id.layoutRestrictionsEditor);
        spinnerAddRestriction = view.findViewById(R.id.spinnerAddRestriction);
        chipGroupNewRestrictions = view.findViewById(R.id.chipGroupNewRestrictions);
        checkPermanentVeda = view.findViewById(R.id.checkPermanentVeda);
        textInputLayoutVedaDays = view.findViewById(R.id.textInputLayoutVedaDays);
        editTextVedaDays = view.findViewById(R.id.editTextVedaDays);
        editTextVedaReturnDate = view.findViewById(R.id.editTextVedaReturnDate);
        editTextRestrictionNotes = view.findViewById(R.id.editTextRestrictionNotes);
        checkScheduleAppointment = view.findViewById(R.id.checkScheduleAppointment);
        layoutAppointmentEditor = view.findViewById(R.id.layoutAppointmentEditor);
        editTextAppointmentDate = view.findViewById(R.id.editTextAppointmentDate);
        editTextAppointmentTime = view.findViewById(R.id.editTextAppointmentTime);
        spinnerAppointmentProcedure = view.findViewById(R.id.spinnerAppointmentProcedure);

        setupMutexLogic();
        setupRestrictionEditor();
        observeViewModel();
        observeSharedDonor();

        donorViewModel.loadRestrictions();

        buttonSaveDonation.setOnClickListener(v -> {
            if (checkApplyRestrictions.isChecked()) {
                handleSaveRestrictions();
            } else {
                handleSave();
            }
        });
        buttonSeeDonorData.setOnClickListener(v -> {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.openDonorDetails();
            }
        });
    }

    private void setupMutexLogic() {
        toggleGroupDonationType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || mutexUpdating) return;

            mutexUpdating = true;
            if (checkedId == R.id.buttonTypeBlood) {
                MaterialButton plasma = group.findViewById(R.id.buttonTypePlasma);
                MaterialButton platelets = group.findViewById(R.id.buttonTypePlatelets);
                plasma.setChecked(false);
                platelets.setChecked(false);
            } else {
                MaterialButton blood = group.findViewById(R.id.buttonTypeBlood);
                blood.setChecked(false);
            }
            mutexUpdating = false;
        });
    }

    private void setupRestrictionEditor() {
        ArrayAdapter<CharSequence> procedureAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.appointment_procedures, android.R.layout.simple_dropdown_item_1line);
        spinnerAppointmentProcedure.setAdapter(procedureAdapter);

        spinnerAddRestriction.setOnItemClickListener((parent, view, position, id) -> {
            MedicalRestriction restriction = dictionaryRestrictions.get(position);
            if (restriction != null && !newRestrictions.containsKey(restriction.getName())) {
                newRestrictions.put(restriction.getName(), restriction.getId());
                addNewRestrictionChip(restriction.getName());
                if (getText(editTextVedaDays).isEmpty() && restriction.getDefaultVedaDays() > 0) {
                    editTextVedaDays.setText(String.valueOf(restriction.getDefaultVedaDays()));
                }
                recomputeReturnDate();
            }
            spinnerAddRestriction.setText("");
        });

        editTextVedaDays.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!suppressVedaWatcher) {
                    recomputeReturnDate();
                }
            }
        });

        checkApplyRestrictions.setOnCheckedChangeListener((buttonView, isChecked) -> applyRestrictionMode(isChecked));
        checkPermanentVeda.setOnCheckedChangeListener((buttonView, isChecked) -> {
            textInputLayoutVedaDays.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            checkScheduleAppointment.setEnabled(!isChecked || selectedAppointment != null);
            recomputeReturnDate();
        });
        checkScheduleAppointment.setOnCheckedChangeListener((buttonView, isChecked) ->
                layoutAppointmentEditor.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        editTextAppointmentDate.setOnClickListener(v -> showAppointmentDatePicker());
        editTextAppointmentTime.setOnClickListener(v -> showAppointmentTimePicker());
    }

    private void applyRestrictionMode(boolean enabled) {
        layoutRestrictionsEditor.setVisibility(enabled ? View.VISIBLE : View.GONE);
        layoutDonationEntry.setVisibility(enabled ? View.GONE : View.VISIBLE);
        buttonSaveDonation.setText(enabled ? R.string.btn_save_restrictions : R.string.btn_save_donation);

        if (!enabled) {
            checkScheduleAppointment.setChecked(false);
            return;
        }
        if (selectedAppointment != null) {
            checkScheduleAppointment.setChecked(true);
        }
        recomputeReturnDate();
    }

    private void addNewRestrictionChip(String name) {
        Chip chip = new Chip(requireContext());
        chip.setText(name);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            chipGroupNewRestrictions.removeView(chip);
            newRestrictions.remove(name);
        });
        chipGroupNewRestrictions.addView(chip);
    }

    private void recomputeReturnDate() {
        if (checkPermanentVeda.isChecked()) {
            setReturnDateText("");
            syncAppointmentDateWithReturn();
            return;
        }
        int days = parseDays();
        if (days <= 0) {
            setReturnDateText("");
            syncAppointmentDateWithReturn();
            return;
        }
        LocalDate returnDate = LocalDate.now().plusDays(days);
        if (returnDate.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) {
            returnDate = returnDate.plusDays(2);
        } else if (returnDate.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            returnDate = returnDate.plusDays(1);
        }
        setReturnDateText(returnDate.toString());
        syncAppointmentDateWithReturn();
    }

    private void setReturnDateText(String dbDate) {
        suppressVedaWatcher = true;
        editTextVedaReturnDate.setText(dbDate);
        suppressVedaWatcher = false;
    }

    private void syncAppointmentDateWithReturn() {
        if (checkScheduleAppointment.isChecked()) {
            String returnDate = getText(editTextVedaReturnDate);
            if (!returnDate.isEmpty()) {
                editTextAppointmentDate.setText(returnDate);
            }
        }
    }

    private void observeDonorRefresh() {
        donorViewModel.getDonorResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.status == Resource.Status.SUCCESS && resource.data != null) {
                sharedDonorViewModel.setSelectedDonor(resource.data);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getSaveResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case LOADING:
                    buttonSaveDonation.setEnabled(false);
                    break;
                case SUCCESS:
                    buttonSaveDonation.setEnabled(true);
                    Toast.makeText(requireContext(), R.string.msg_save_donation, Toast.LENGTH_LONG).show();
                    clearClinicalForm();
                    if (selectedDonor != null) {
                        donorViewModel.getDonorByDni(selectedDonor.getDni());
                    }
                    break;
                case ERROR:
                    buttonSaveDonation.setEnabled(true);
                    if (resource.message != null) {
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        });

        donorViewModel.getRestrictionsResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS) {
                dictionaryRestrictions.clear();
                if (resource.data != null) {
                    dictionaryRestrictions.addAll(resource.data);
                }
                List<String> names = new ArrayList<>();
                for (MedicalRestriction r : dictionaryRestrictions) {
                    names.add(r.getName());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, names);
                spinnerAddRestriction.setAdapter(adapter);
            }
        });

        donorViewModel.getRestrictionActionResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case LOADING:
                    buttonSaveDonation.setEnabled(false);
                    break;
                case SUCCESS:
                    buttonSaveDonation.setEnabled(true);
                    Toast.makeText(requireContext(), R.string.msg_restrictions_saved, Toast.LENGTH_LONG).show();
                    resetRestrictionEditor();
                    refreshDonor();
                    break;
                case ERROR:
                    buttonSaveDonation.setEnabled(true);
                    if (resource.message != null) {
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        });
    }

    private void resetRestrictionEditor() {
        newRestrictions.clear();
        chipGroupNewRestrictions.removeAllViews();
        spinnerAddRestriction.setText("");
        suppressVedaWatcher = true;
        editTextVedaDays.setText("");
        editTextVedaReturnDate.setText("");
        editTextRestrictionNotes.setText("");
        suppressVedaWatcher = false;
        checkPermanentVeda.setChecked(false);
        checkScheduleAppointment.setChecked(false);
        editTextAppointmentDate.setText("");
        editTextAppointmentTime.setText("");
        spinnerAppointmentProcedure.setText("");
        checkApplyRestrictions.setChecked(false);
    }

    private void refreshDonor() {
        if (selectedDonor != null && selectedDonor.getDni() != null) {
            donorViewModel.getDonorByDni(selectedDonor.getDni());
        }
    }

    private void observeSharedDonor() {
        sharedDonorViewModel.getSelectedDonor().observe(getViewLifecycleOwner(), donor -> {
            selectedDonor = donor;
            if (donor == null) {
                textViewDonorContextName.setText(R.string.hint_select_donor);
                textViewDonorContextDni.setText("");
                textViewDonorContextBlood.setText("");
                textViewDonorContextStaff.setText("");
                chipDonorContextStatus.setVisibility(View.GONE);
                buttonSeeDonorData.setEnabled(false);
                layoutActiveRestrictions.removeAllViews();
                textViewNoActiveRestrictions.setVisibility(View.GONE);
                checkApplyRestrictions.setEnabled(false);
            } else {
                textViewDonorContextName.setText(buildFullName(donor));
                textViewDonorContextDni.setText(getString(R.string.hint_dni) + " " + safe(donor.getDni()));
                textViewDonorContextBlood.setText(safe(donor.getBloodType()));
                textViewDonorContextStaff.setText("Atendido por: " + SessionManager.getInstance(requireContext()).getFullName());
                bindBadge(donor);
                buttonSeeDonorData.setEnabled(true);
                checkApplyRestrictions.setEnabled(true);
                selectedAppointment = sharedDonorViewModel.consumeSelectedAppointment();
                configureAppointmentMode();
                renderActiveRestrictions(donor);
                applyPreselectedProcedure();
                applyRestrictionMode(checkApplyRestrictions.isChecked());
            }
        });
    }

    private void configureAppointmentMode() {
        boolean hasAppointment = selectedAppointment != null;
        checkScheduleAppointment.setText(hasAppointment
                ? R.string.label_reschedule_appointment
                : R.string.label_schedule_appointment);
        checkScheduleAppointment.setChecked(false);
        layoutAppointmentEditor.setVisibility(View.GONE);
    }

    private void renderActiveRestrictions(Donor donor) {
        layoutActiveRestrictions.removeAllViews();
        List<MedicalRestriction> active = donor.getActiveRestrictions();
        if (active == null || active.isEmpty()) {
            textViewNoActiveRestrictions.setVisibility(View.VISIBLE);
            return;
        }
        textViewNoActiveRestrictions.setVisibility(View.GONE);
        for (MedicalRestriction restriction : active) {
            View row = getLayoutInflater().inflate(R.layout.item_active_restriction, layoutActiveRestrictions, false);
            TextView name = row.findViewById(R.id.textViewRestrictionName);
            TextView until = row.findViewById(R.id.textViewRestrictionUntil);
            TextView notes = row.findViewById(R.id.textViewRestrictionNotes);

            name.setText(restriction.getName());
            if (restriction.isPermanent()) {
                until.setText(R.string.restriction_permanent_label);
            } else {
                until.setText(getString(R.string.restriction_until_format,
                        formatDisplayDate(restriction.getEligibleWorkdayReturnDate())));
            }
            if (restriction.getNotes() != null && !restriction.getNotes().trim().isEmpty()) {
                notes.setText(restriction.getNotes());
                notes.setVisibility(View.VISIBLE);
            }

            row.findViewById(R.id.buttonEditRestriction).setOnClickListener(v -> openEditRestrictionDialog(restriction));
            row.findViewById(R.id.buttonRemoveRestriction).setOnClickListener(v -> confirmRemoveRestriction(restriction));
            layoutActiveRestrictions.addView(row);
        }
    }

    private void applyPreselectedProcedure() {
        String procedure = sharedDonorViewModel.consumeSelectedProcedure();
        if (procedure == null) {
            return;
        }
        int targetId;
        switch (procedure) {
            case "PLASMA":
                targetId = R.id.buttonTypePlasma;
                break;
            case "PLATELETS":
                targetId = R.id.buttonTypePlatelets;
                break;
            case "BLOOD":
                targetId = R.id.buttonTypeBlood;
                break;
            default:
                return;
        }
        mutexUpdating = true;
        toggleGroupDonationType.clearChecked();
        mutexUpdating = false;
        toggleGroupDonationType.check(targetId);
    }

    private void bindBadge(Donor donor) {
        boolean enVeda = "EN_VEDA".equals(donor.getStatus());
        if (enVeda) {
            chipDonorContextStatus.setText(R.string.badge_veda);
            chipDonorContextStatus.setChipBackgroundColorResource(R.color.veda_red);
        } else {
            chipDonorContextStatus.setText(R.string.badge_apto);
            chipDonorContextStatus.setChipBackgroundColorResource(R.color.aptitude_green);
        }
        chipDonorContextStatus.setVisibility(View.VISIBLE);
    }

    private String buildFullName(Donor donor) {
        return (safe(donor.getLastName()) + ", " + safe(donor.getFirstName())).trim();
    }

    private void clearClinicalForm() {
        editTextHto.setText("");
        editTextPlateletsCount.setText("");
        toggleGroupDonationType.check(R.id.buttonTypePlasma);
    }

    // --- Donation save ---

    private void handleSave() {
        if (selectedDonor == null || selectedDonor.getId() <= 0) {
            Toast.makeText(requireContext(), R.string.msg_select_donor_first, Toast.LENGTH_LONG).show();
            return;
        }

        if (!validateClinicalFields()) {
            return;
        }

        List<String> procedures = getCheckedProcedures();
        if (procedures.isEmpty()) {
            Toast.makeText(requireContext(), R.string.msg_select_procedure, Toast.LENGTH_LONG).show();
            return;
        }

        Donation donation = new Donation(
                selectedDonor.getId(),
                donorWeight(),
                donorHeight(),
                parseDouble(editTextHto),
                (int) parseDouble(editTextPlateletsCount),
                SessionManager.getInstance(requireContext()).getUserIdOrNull(),
                procedures
        );

        List<String> warnings = buildRecoveryWarnings(procedures);
        if (warnings.isEmpty()) {
            viewModel.saveDonation(donation);
        } else {
            showRecoveryWarningDialog(donation, warnings);
        }
    }

    private List<String> buildRecoveryWarnings(List<String> procedures) {
        List<String> warnings = new ArrayList<>();
        String nextEligible = safe(selectedDonor.getNextEligibleDate());
        if (!nextEligible.isEmpty()) {
            try {
                if (LocalDate.parse(nextEligible).isAfter(LocalDate.now())) {
                    warnings.add(getString(R.string.warning_recovery_not_elapsed, formatDisplayDate(nextEligible)));
                }
            } catch (Exception ignored) {
            }
        }
        Map<String, AnnualCount> counts = selectedDonor.getAnnualCounts();
        if (counts != null) {
            for (String code : procedures) {
                AnnualCount entry = counts.get(code);
                if (entry != null && entry.getCount() >= entry.getMax()) {
                    warnings.add(getString(
                            R.string.warning_annual_limit,
                            procedureLabel(code),
                            String.valueOf(entry.getCount()),
                            String.valueOf(entry.getMax())));
                }
            }
        }
        return warnings;
    }

    private void showRecoveryWarningDialog(Donation donation, List<String> warnings) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_warning_title)
                .setMessage(TextUtils.join("\n\n", warnings))
                .setCancelable(false)
                .setPositiveButton(R.string.btn_register_anyway, (dialog, which) -> viewModel.saveDonation(donation))
                .setNegativeButton(R.string.btn_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    // --- Restrictions save ---

    private void handleSaveRestrictions() {
        if (selectedDonor == null || selectedDonor.getId() <= 0) {
            Toast.makeText(requireContext(), R.string.msg_select_donor_first, Toast.LENGTH_LONG).show();
            return;
        }
        if (newRestrictions.isEmpty()) {
            Toast.makeText(requireContext(), R.string.msg_select_restriction, Toast.LENGTH_LONG).show();
            return;
        }
        boolean permanent = checkPermanentVeda.isChecked();
        int vedaDays = parseDays();
        if (!permanent && vedaDays <= 0) {
            Toast.makeText(requireContext(), R.string.msg_veda_days_required, Toast.LENGTH_LONG).show();
            return;
        }

        RescheduleRequest reschedule = buildRescheduleFromEditor();
        if (checkScheduleAppointment.isChecked() && reschedule == null) {
            return;
        }

        List<AssignRestrictionRequest> requests = new ArrayList<>();
        String notes = getText(editTextRestrictionNotes);
        for (Map.Entry<String, Integer> entry : newRestrictions.entrySet()) {
            requests.add(new AssignRestrictionRequest(entry.getValue(), vedaDays, notes, permanent));
        }

        AssignRestrictionsRequest request = new AssignRestrictionsRequest(
                selectedDonor.getId(),
                SessionManager.getInstance(requireContext()).getUserIdOrNull(),
                requests,
                reschedule);
        donorViewModel.assignRestrictions(request);
    }

    private RescheduleRequest buildRescheduleFromEditor() {
        if (!checkScheduleAppointment.isChecked()) {
            return null;
        }
        String date = getText(editTextAppointmentDate);
        String time = getText(editTextAppointmentTime);
        String procedure = procedureCodeFromLabel(getText(spinnerAppointmentProcedure));
        if (date.isEmpty()) {
            Toast.makeText(requireContext(), R.string.schedule_date_picker_title, Toast.LENGTH_SHORT).show();
            return null;
        }
        if (time.isEmpty()) {
            Toast.makeText(requireContext(), R.string.schedule_time_picker_title, Toast.LENGTH_SHORT).show();
            return null;
        }
        if (procedure == null) {
            Toast.makeText(requireContext(), R.string.msg_select_procedure, Toast.LENGTH_SHORT).show();
            return null;
        }
        Integer cancelId = selectedAppointment != null ? selectedAppointment.getId() : null;
        return new RescheduleRequest(cancelId, date, time, procedure);
    }

    private void confirmRemoveRestriction(MedicalRestriction restriction) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_remove_restriction_title)
                .setMessage(getString(R.string.dialog_remove_restriction_message,
                        selectedDonor != null ? buildFullName(selectedDonor) : "",
                        restriction.getName()))
                .setPositiveButton(R.string.btn_remove, (dialog, which) ->
                        donorViewModel.revokeRestriction(new RevokeRestrictionRequest(restriction.getId(), null)))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void openEditRestrictionDialog(MedicalRestriction restriction) {
        View content = getLayoutInflater().inflate(R.layout.dialog_restriction_edit, null, false);
        TextInputEditText nameField = content.findViewById(R.id.editTextEditRestrictionName);
        MaterialCheckBox permanentBox = content.findViewById(R.id.checkEditPermanent);
        TextInputLayout daysLayout = content.findViewById(R.id.textInputLayoutEditVedaDays);
        TextInputEditText daysField = content.findViewById(R.id.editTextEditVedaDays);
        TextInputEditText returnField = content.findViewById(R.id.editTextEditVedaReturnDate);
        TextInputEditText notesField = content.findViewById(R.id.editTextEditNotes);
        MaterialCheckBox scheduleBox = content.findViewById(R.id.checkEditScheduleAppointment);
        LinearLayout appointmentLayout = content.findViewById(R.id.layoutEditAppointmentEditor);
        TextInputEditText appointmentDate = content.findViewById(R.id.editTextEditAppointmentDate);
        TextInputEditText appointmentTime = content.findViewById(R.id.editTextEditAppointmentTime);
        AutoCompleteTextView appointmentProcedure = content.findViewById(R.id.spinnerEditAppointmentProcedure);

        ArrayAdapter<CharSequence> procedureAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.appointment_procedures, android.R.layout.simple_dropdown_item_1line);
        appointmentProcedure.setAdapter(procedureAdapter);

        nameField.setText(restriction.getName());
        permanentBox.setChecked(restriction.isPermanent());
        daysField.setText(restriction.getVedaDays() > 0 ? String.valueOf(restriction.getVedaDays()) : "");
        returnField.setText(restriction.getEligibleWorkdayReturnDate() != null
                ? formatDisplayDate(restriction.getEligibleWorkdayReturnDate()) : "");
        notesField.setText(restriction.getNotes() != null ? restriction.getNotes() : "");
        daysLayout.setVisibility(restriction.isPermanent() ? View.GONE : View.VISIBLE);

        scheduleBox.setText(selectedAppointment != null
                ? R.string.label_reschedule_appointment
                : R.string.label_schedule_appointment);

        permanentBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            daysLayout.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            recomputeReturnDate(daysField, returnField);
        });
        daysField.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                recomputeReturnDate(daysField, returnField);
            }
        });
        scheduleBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                appointmentLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        appointmentDate.setOnClickListener(v -> showDatePicker(appointmentDate));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_edit_restriction_title)
                .setView(content)
                .setPositiveButton(R.string.btn_accept, null)
                .setNegativeButton(R.string.btn_cancel, null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean permanent = permanentBox.isChecked();
            int days = parseDays(daysField);
            if (!permanent && days <= 0) {
                Toast.makeText(requireContext(), R.string.msg_veda_days_required, Toast.LENGTH_SHORT).show();
                return;
            }
            RescheduleRequest reschedule = null;
            if (scheduleBox.isChecked()) {
                String date = getText(appointmentDate);
                String time = getText(appointmentTime);
                String procedure = procedureCodeFromLabel(getText(appointmentProcedure));
                if (date.isEmpty() || time.isEmpty() || procedure == null) {
                    Toast.makeText(requireContext(), R.string.msg_validation_donation, Toast.LENGTH_SHORT).show();
                    return;
                }
                Integer cancelId = selectedAppointment != null ? selectedAppointment.getId() : null;
                reschedule = new RescheduleRequest(cancelId, date, time, procedure);
            }

            UpdateRestrictionRequest request = new UpdateRestrictionRequest(
                    restriction.getId(),
                    restriction.getRestrictionId(),
                    days,
                    permanent,
                    getText(notesField),
                    restriction.getStartDate() != null ? restriction.getStartDate() : LocalDate.now().toString());
            request.setReschedule(reschedule);
            donorViewModel.updateRestriction(request);
            dialog.dismiss();
        });
    }

    private void recomputeReturnDate(TextInputEditText daysField, TextInputEditText returnField) {
        int days = parseDays(daysField);
        if (days <= 0) {
            returnField.setText("");
            return;
        }
        LocalDate returnDate = LocalDate.now().plusDays(days);
        if (returnDate.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) {
            returnDate = returnDate.plusDays(2);
        } else if (returnDate.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            returnDate = returnDate.plusDays(1);
        }
        returnField.setText(returnDate.toString());
    }

    private void showAppointmentDatePicker() {
        showDatePicker(editTextAppointmentDate);
    }

    private void showDatePicker(TextInputEditText target) {
        CalendarConstraints.DateValidator validator =
                DateValidatorPointForward.from(MaterialDatePicker.todayInUtcMilliseconds());
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.schedule_date_picker_title))
                .setCalendarConstraints(new CalendarConstraints.Builder().setValidator(validator).build())
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            LocalDate date = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
            target.setText(date.format(DB_DATE));
        });
        picker.show(getParentFragmentManager(), "appointmentDatePicker");
    }

    private void showAppointmentTimePicker() {
        TimePickerDialog timePicker = new TimePickerDialog(requireContext(),
                (dialog, hourOfDay, minute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    editTextAppointmentTime.setText(time);
                }, 8, 0, true);
        timePicker.setTitle(getString(R.string.schedule_time_picker_title));
        timePicker.show();
    }

    private String formatDisplayDate(String dbDate) {
        if (dbDate == null || dbDate.trim().isEmpty()) {
            return "";
        }
        String normalized = dbDate.contains(" ") ? dbDate.substring(0, 10) : dbDate;
        try {
            return LocalDate.parse(normalized).format(DISPLAY_DATE);
        } catch (Exception e) {
            return normalized;
        }
    }

    private String procedureLabel(String code) {
        if ("BLOOD".equals(code)) {
            return getString(R.string.procedure_blood);
        }
        if ("PLASMA".equals(code)) {
            return getString(R.string.procedure_plasma);
        }
        if ("PLATELETS".equals(code)) {
            return getString(R.string.procedure_platelets);
        }
        return code;
    }

    private String procedureCodeFromLabel(String label) {
        if (label == null) {
            return null;
        }
        switch (label.trim().toLowerCase(Locale.getDefault())) {
            case "plasma":
                return "PLASMA";
            case "plaquetas":
                return "PLATELETS";
            case "sangre":
                return "BLOOD";
            default:
                return null;
        }
    }

    private double donorWeight() {
        return selectedDonor != null && selectedDonor.getWeight() != null ? selectedDonor.getWeight() : 0.0;
    }

    private double donorHeight() {
        return selectedDonor != null && selectedDonor.getHeight() != null ? selectedDonor.getHeight() : 0.0;
    }

    private boolean validateClinicalFields() {
        if (parseDouble(editTextHto) <= 0 || parseDouble(editTextPlateletsCount) <= 0) {
            Toast.makeText(requireContext(), R.string.msg_validation_donation, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private List<String> getCheckedProcedures() {
        List<String> procedures = new ArrayList<>();
        if (toggleGroupDonationType.getCheckedButtonIds().contains(R.id.buttonTypePlasma)) {
            procedures.add("PLASMA");
        }
        if (toggleGroupDonationType.getCheckedButtonIds().contains(R.id.buttonTypePlatelets)) {
            procedures.add("PLATELETS");
        }
        if (toggleGroupDonationType.getCheckedButtonIds().contains(R.id.buttonTypeBlood)) {
            procedures.add("BLOOD");
        }
        return procedures;
    }

    private double parseDouble(TextInputEditText edit) {
        String text = edit.getText() != null ? edit.getText().toString().trim() : "";
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parseDays() {
        return parseDays(editTextVedaDays);
    }

    private int parseDays(TextInputEditText edit) {
        try {
            return Integer.parseInt(getText(edit));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getText(TextInputEditText edit) {
        return edit != null && edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    private String getText(TextView textView) {
        return textView != null && textView.getText() != null ? textView.getText().toString().trim() : "";
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
