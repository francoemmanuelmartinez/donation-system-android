package com.francomartinez.donationsystem.views.fragments;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.models.AnnualCount;
import com.francomartinez.donationsystem.models.ContactEmail;
import com.francomartinez.donationsystem.models.ContactPhone;
import com.francomartinez.donationsystem.models.Donor;
import com.francomartinez.donationsystem.models.MedicalRestriction;
import com.francomartinez.donationsystem.models.requests.AssignRestrictionRequest;
import com.francomartinez.donationsystem.utils.KeyboardUtils;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.utils.SessionManager;
import com.francomartinez.donationsystem.viewmodels.DonorsViewModel;
import com.francomartinez.donationsystem.viewmodels.SharedDonorViewModel;
import com.francomartinez.donationsystem.views.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class DonorFormFragment extends Fragment {

    private static final String ARG_MODE = "arg_mode";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final int MODE_REGISTER = 0;
    private static final int MODE_VIEW = 1;
    private static final int MODE_MODIFY = 2;

    private static final int MODE_PHONE = 0;
    private static final int MODE_EMAIL = 1;

    private TextInputEditText editTextLastName;
    private TextInputEditText editTextFirstName;
    private TextInputEditText editTextDni;
    private AutoCompleteTextView spinnerGender;
    private TextInputEditText editTextBirthDate;
    private TextInputLayout textInputLayoutBirthDate;
    private TextInputEditText editTextAge;
    private TextInputEditText editTextCity;
    private TextInputEditText editTextAddress;
    private LinearLayout layoutPhoneRows;
    private LinearLayout layoutEmailRows;
    private AutoCompleteTextView spinnerBloodType;
    private TextInputEditText editTextPhenotype;
    private TextInputEditText editTextWeight;
    private TextInputEditText editTextHeight;
    private TextInputLayout textInputLayoutLastDonationDate;
    private TextInputEditText editTextLastDonationDate;
    private TextInputLayout textInputLayoutNextEligibleDate;
    private TextInputEditText editTextNextEligibleDate;
    private TextView textViewAnnualCounts;
    private TextInputLayout textInputLayoutAptitude;
    private AutoCompleteTextView spinnerAptitude;
    private LinearLayout layoutExclusionDetails;
    private TextInputEditText editTextVedaReturnDate;
    private AutoCompleteTextView spinnerAddRestriction;
    private TextInputEditText editTextVedaDays;
    private ChipGroup chipGroupRestrictions;
    private TextInputEditText editTextRestrictionNotes;
    private MaterialButton buttonAddPhone;
    private MaterialButton buttonAddEmail;
    private MaterialButton buttonDonorAction;

    private DonorsViewModel viewModel;
    private SharedDonorViewModel sharedDonorViewModel;

    private int mode = MODE_REGISTER;
    private Donor currentDonor;
    private final List<MedicalRestriction> dictionaryRestrictions = new ArrayList<>();
    private final LinkedHashMap<String, Integer> selectedRestrictions = new LinkedHashMap<>();
    private final java.util.Set<String> alreadyActiveNames = new java.util.HashSet<>();

    public static DonorFormFragment newRegisterInstance() {
        return newInstance(MODE_REGISTER);
    }

    public static DonorFormFragment newViewInstance() {
        return newInstance(MODE_VIEW);
    }

    private static DonorFormFragment newInstance(int mode) {
        DonorFormFragment fragment = new DonorFormFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_donor_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            mode = getArguments().getInt(ARG_MODE, MODE_REGISTER);
        }

        viewModel = new ViewModelProvider(this).get(DonorsViewModel.class);
        sharedDonorViewModel = new ViewModelProvider(requireActivity()).get(SharedDonorViewModel.class);

        bindViews(view);
        setupDropdowns();
        setupVedaDaysWatcher();
        observeViewModel();

        editTextBirthDate.setOnClickListener(v -> showDatePicker());
        editTextBirthDate.setKeyListener(null);
        if (textInputLayoutBirthDate != null) {
            textInputLayoutBirthDate.setOnClickListener(v -> {
                if (editTextBirthDate.isEnabled()) {
                    showDatePicker();
                }
            });
        }

        buttonAddPhone.setOnClickListener(v -> addContactRow(MODE_PHONE, "", null, true));
        buttonAddEmail.setOnClickListener(v -> addContactRow(MODE_EMAIL, "", null, true));

        buttonDonorAction.setOnClickListener(v -> {
            if (mode == MODE_VIEW) {
                setMode(MODE_MODIFY);
            } else {
                handleSave();
            }
        });

        viewModel.loadRestrictions();
        requestedIfEmptyContacts();

        if (mode == MODE_VIEW || mode == MODE_MODIFY) {
            Donor donor = sharedDonorViewModel.getSelectedDonor().getValue();
            if (donor != null) {
                currentDonor = donor;
                bindDonor(donor);
            }
        }
        setMode(mode);
    }

    private void bindViews(View view) {
        editTextLastName = view.findViewById(R.id.editTextLastName);
        editTextFirstName = view.findViewById(R.id.editTextFirstName);
        editTextDni = view.findViewById(R.id.editTextDni);
        spinnerGender = view.findViewById(R.id.spinnerGender);
        editTextBirthDate = view.findViewById(R.id.editTextBirthDate);
        textInputLayoutBirthDate = view.findViewById(R.id.textInputLayoutBirthDate);
        editTextAge = view.findViewById(R.id.editTextAge);
        editTextCity = view.findViewById(R.id.editTextCity);
        editTextAddress = view.findViewById(R.id.editTextAddress);
        layoutPhoneRows = view.findViewById(R.id.layoutPhoneRows);
        layoutEmailRows = view.findViewById(R.id.layoutEmailRows);
        spinnerBloodType = view.findViewById(R.id.spinnerBloodType);
        editTextPhenotype = view.findViewById(R.id.editTextPhenotype);
        editTextWeight = view.findViewById(R.id.editTextWeight);
        editTextHeight = view.findViewById(R.id.editTextHeight);
        textInputLayoutLastDonationDate = view.findViewById(R.id.textInputLayoutLastDonationDate);
        editTextLastDonationDate = view.findViewById(R.id.editTextLastDonationDate);
        textInputLayoutNextEligibleDate = view.findViewById(R.id.textInputLayoutNextEligibleDate);
        editTextNextEligibleDate = view.findViewById(R.id.editTextNextEligibleDate);
        textViewAnnualCounts = view.findViewById(R.id.textViewAnnualCounts);
        textInputLayoutAptitude = view.findViewById(R.id.textInputLayoutAptitude);
        spinnerAptitude = view.findViewById(R.id.spinnerAptitude);
        layoutExclusionDetails = view.findViewById(R.id.layoutExclusionDetails);
        editTextVedaReturnDate = view.findViewById(R.id.editTextVedaReturnDate);
        spinnerAddRestriction = view.findViewById(R.id.spinnerAddRestriction);
        editTextVedaDays = view.findViewById(R.id.editTextVedaDays);
        chipGroupRestrictions = view.findViewById(R.id.chipGroupRestrictions);
        editTextRestrictionNotes = view.findViewById(R.id.editTextRestrictionNotes);
        buttonAddPhone = view.findViewById(R.id.buttonAddPhone);
        buttonAddEmail = view.findViewById(R.id.buttonAddEmail);
        buttonDonorAction = view.findViewById(R.id.buttonDonorAction);
    }

    private void setupDropdowns() {
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.genders, android.R.layout.simple_dropdown_item_1line);
        spinnerGender.setAdapter(genderAdapter);

        ArrayAdapter<CharSequence> bloodAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.blood_types, android.R.layout.simple_dropdown_item_1line);
        spinnerBloodType.setAdapter(bloodAdapter);

        ArrayAdapter<CharSequence> aptitudeAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.aptitudes, android.R.layout.simple_dropdown_item_1line);
        spinnerAptitude.setAdapter(aptitudeAdapter);
        spinnerAptitude.setText(getString(R.string.aptitude_apto), false);
        spinnerAptitude.setOnItemClickListener((parent, view, position, id) -> applyAptitudeState());

        spinnerAddRestriction.setOnItemClickListener((parent, view, position, id) -> {
            MedicalRestriction restriction = dictionaryRestrictions.get(position);
            if (restriction != null && !selectedRestrictions.containsKey(restriction.getName())) {
                selectedRestrictions.put(restriction.getName(), restriction.getId());
                addRestrictionChip(restriction.getName(), true);
                spinnerAddRestriction.setText("");
                applyAptitudeState();
            }
            KeyboardUtils.hide(spinnerAddRestriction);
        });
    }

    private void setupVedaDaysWatcher() {
        editTextVedaDays.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                recomputeReturnDate();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getDonorResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                currentDonor = resource.data;
                sharedDonorViewModel.setSelectedDonor(currentDonor);
                if (mode == MODE_REGISTER) {
                    navigateToClinicalForm();
                    return;
                }
                bindDonor(currentDonor);
                if (mode == MODE_MODIFY) {
                    setMode(MODE_VIEW);
                }
            } else if (resource.status == Resource.Status.ERROR) {
                if (resource.message != null) {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                }
            }
        });

        viewModel.getSaveResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case LOADING:
                    buttonDonorAction.setEnabled(false);
                    break;
                case SUCCESS:
                    buttonDonorAction.setEnabled(true);
                    int donorId = extractDonorId(resource.data, currentDonor != null ? currentDonor.getId() : -1);
                    List<AssignRestrictionRequest> pending = buildPendingRestrictionRequests(donorId);
                    if (!pending.isEmpty()) {
                        viewModel.assignRestrictions(pending);
                    } else {
                        Toast.makeText(requireContext(), R.string.msg_donor_saved, Toast.LENGTH_SHORT).show();
                        refreshAfterSave();
                    }
                    break;
                case ERROR:
                    buttonDonorAction.setEnabled(true);
                    if (resource.message != null) {
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        });

        viewModel.getRestrictionsResult().observe(getViewLifecycleOwner(), resource -> {
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

        viewModel.getAssignResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS) {
                Toast.makeText(requireContext(), R.string.msg_restrictions_updated, Toast.LENGTH_SHORT).show();
                refreshAfterSave();
            } else if (resource.status == Resource.Status.ERROR && resource.message != null) {
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                refreshAfterSave();
            }
        });
    }

    private void refreshAfterSave() {
        String dni = getText(editTextDni);
        if (!dni.isEmpty()) {
            viewModel.getDonorByDni(dni);
        }
    }

    private void navigateToClinicalForm() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openClinicalForm();
        }
    }

    private int extractDonorId(Map<String, Object> data, int fallback) {
        if (data != null) {
            Object id = data.get("donorId");
            if (id instanceof Number) {
                return ((Number) id).intValue();
            }
        }
        return fallback;
    }

    private List<AssignRestrictionRequest> buildPendingRestrictionRequests(int donorId) {
        Integer userId = getSessionUserId();
        String notes = getText(editTextRestrictionNotes);
        int vedaDays = parseDays();
        List<AssignRestrictionRequest> requests = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : selectedRestrictions.entrySet()) {
            if (alreadyActiveNames.contains(entry.getKey())) {
                continue;
            }
            requests.add(new AssignRestrictionRequest(donorId, entry.getValue(), vedaDays, notes, userId));
        }
        return requests;
    }

    private Integer getSessionUserId() {
        SessionManager session = SessionManager.getInstance(requireContext());
        return session.getUserIdOrNull();
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.hint_birth_date))
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            String dbDate = formatMillisToDbDate(selection);
            editTextBirthDate.setText(dbDate);
            updateAge(dbDate);
        });
        picker.show(getParentFragmentManager(), "birthDatePicker");
    }

    private String formatMillisToDbDate(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(millis));
    }

    private void updateAge(String birthDate) {
        try {
            LocalDate birth = LocalDate.parse(birthDate, DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US));
            long years = ChronoUnit.YEARS.between(birth, LocalDate.now());
            editTextAge.setText(String.valueOf(years));
        } catch (Exception e) {
            editTextAge.setText("");
        }
    }

    private void bindDonor(Donor donor) {
        if (donor == null) return;

        editTextLastName.setText(safe(donor.getLastName()));
        editTextFirstName.setText(safe(donor.getFirstName()));
        editTextDni.setText(safe(donor.getDni()));
        spinnerGender.setText(safe(donor.getGender()), false);
        editTextBirthDate.setText(safe(donor.getBirthDate()));
        updateAge(donor.getBirthDate());
        editTextCity.setText(safe(donor.getCity()));
        editTextAddress.setText(safe(donor.getAddress()));
        spinnerBloodType.setText(safe(donor.getBloodType()), false);
        editTextPhenotype.setText(safe(donor.getPhenotype()));
        editTextWeight.setText(formatDecimal(donor.getWeight()));
        editTextHeight.setText(formatDecimal(donor.getHeight()));
        editTextLastDonationDate.setText(normalizeDonationDate(donor.getLastDonationDate()));
        bindRecoveryInfo(donor);

        layoutPhoneRows.removeAllViews();
        layoutEmailRows.removeAllViews();
        if (donor.getPhones() != null) {
            for (ContactPhone p : donor.getPhones()) {
                addContactRow(MODE_PHONE, p.getPhoneNumber(), p.getPhoneType(), false);
            }
        }
        if (donor.getEmails() != null) {
            for (ContactEmail e : donor.getEmails()) {
                addContactRow(MODE_EMAIL, e.getEmailAddress(), e.getEmailType(), false);
            }
        }
        requestedIfEmptyContacts();

        selectedRestrictions.clear();
        alreadyActiveNames.clear();
        chipGroupRestrictions.removeAllViews();
        if (donor.getActiveRestrictions() != null && !donor.getActiveRestrictions().isEmpty()) {
            int maxVedaDays = 0;
            String maxReturnDate = "";
            for (MedicalRestriction r : donor.getActiveRestrictions()) {
                alreadyActiveNames.add(r.getName());
                addRestrictionChip(r.getName(), false);
                if (r.getVedaDays() > maxVedaDays) {
                    maxVedaDays = r.getVedaDays();
                }
                if (r.getEligibleWorkdayReturnDate() != null
                        && r.getEligibleWorkdayReturnDate().compareTo(maxReturnDate) > 0) {
                    maxReturnDate = r.getEligibleWorkdayReturnDate();
                }
                if (getText(editTextRestrictionNotes).isEmpty() && r.getNotes() != null) {
                    editTextRestrictionNotes.setText(r.getNotes());
                }
            }
            if (maxVedaDays > 0) {
                editTextVedaDays.setText(String.valueOf(maxVedaDays));
            } else {
                editTextVedaDays.setText("");
            }
            editTextVedaReturnDate.setText(maxReturnDate);
        } else {
            editTextVedaDays.setText("");
            editTextVedaReturnDate.setText("");
        }

        syncAptitudeWithDonor();
    }

    private void bindRecoveryInfo(Donor donor) {
        String nextEligible = normalizeDonationDate(donor.getNextEligibleDate());
        if (nextEligible.isEmpty()) {
            if (textInputLayoutNextEligibleDate != null) {
                textInputLayoutNextEligibleDate.setVisibility(View.GONE);
            }
        } else {
            editTextNextEligibleDate.setText(formatDisplayDate(nextEligible));
            boolean future = false;
            try {
                future = LocalDate.parse(nextEligible).isAfter(LocalDate.now());
            } catch (Exception ignored) {
            }
            editTextNextEligibleDate.setTextColor(getResources().getColor(
                    future ? R.color.aptitude_red_soft : R.color.clinical_dark_blue_light));
            if (textInputLayoutNextEligibleDate != null) {
                textInputLayoutNextEligibleDate.setVisibility(View.VISIBLE);
            }
        }

        Map<String, AnnualCount> annualCounts = donor.getAnnualCounts();
        if (annualCounts == null || annualCounts.isEmpty()) {
            if (textViewAnnualCounts != null) {
                textViewAnnualCounts.setVisibility(View.GONE);
            }
            return;
        }

        boolean exceeded = false;
        StringBuilder summary = new StringBuilder(getString(R.string.label_annual_counts) + " ");
        int index = 0;
        for (String code : new String[]{"BLOOD", "PLASMA", "PLATELETS"}) {
            AnnualCount entry = annualCounts.get(code);
            if (entry == null) {
                continue;
            }
            if (index > 0) {
                summary.append(" · ");
            }
            summary.append(procedureLabel(code)).append(" ")
                    .append(entry.getCount()).append("/").append(entry.getMax());
            if (entry.getCount() >= entry.getMax()) {
                exceeded = true;
            }
            index++;
        }
        if (index == 0) {
            textViewAnnualCounts.setVisibility(View.GONE);
            return;
        }
        textViewAnnualCounts.setText(summary.toString());
        textViewAnnualCounts.setTextColor(getResources().getColor(
                exceeded ? R.color.aptitude_red_soft : R.color.clinical_dark_blue_light));
        textViewAnnualCounts.setVisibility(View.VISIBLE);
    }

    private String formatDisplayDate(String dbDate) {
        try {
            return LocalDate.parse(dbDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()));
        } catch (Exception e) {
            return dbDate;
        }
    }

    private String procedureLabel(String code) {
        switch (code) {
            case "BLOOD":
                return getString(R.string.procedure_blood);
            case "PLASMA":
                return getString(R.string.procedure_plasma);
            case "PLATELETS":
                return getString(R.string.procedure_platelets);
            default:
                return code;
        }
    }

    private String normalizeDonationDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        if (raw.contains(" ")) {
            return raw.substring(0, Math.min(10, raw.length()));
        }
        return raw;
    }

    private void addRestrictionChip(String name, boolean closeable) {
        Chip chip = new Chip(requireContext());
        chip.setText(name);
        chip.setCloseIconVisible(closeable);
        if (closeable) {
            chip.setOnCloseIconClickListener(v -> {
                chipGroupRestrictions.removeView(chip);
                selectedRestrictions.remove(name);
                alreadyActiveNames.remove(name);
                updateBadge();
            });
        }
        chipGroupRestrictions.addView(chip);
    }

    private void updateBadge() {
        if (selectedRestrictions.isEmpty()) {
            spinnerAptitude.setText(getString(R.string.aptitude_apto), false);
        }
        applyAptitudeState();
    }

    private void syncAptitudeWithDonor() {
        boolean enVeda = currentDonor != null && "EN_VEDA".equals(currentDonor.getStatus());
        spinnerAptitude.setText(enVeda ? getString(R.string.aptitude_no_apto) : getString(R.string.aptitude_apto), false);
        applyAptitudeState();
    }

    private void applyAptitudeState() {
        if (spinnerAptitude == null || layoutExclusionDetails == null || textInputLayoutAptitude == null) {
            return;
        }
        boolean noApto = getString(R.string.aptitude_no_apto).equals(getText(spinnerAptitude));
        layoutExclusionDetails.setVisibility(noApto ? View.VISIBLE : View.GONE);

        int strokeColor = getResources().getColor(noApto ? R.color.veda_red : R.color.aptitude_green);
        int labelColor = getResources().getColor(noApto ? R.color.aptitude_red_soft : R.color.aptitude_green_soft);
        textInputLayoutAptitude.setBoxBackgroundColor(getResources().getColor(R.color.navy_field));
        int[][] states = new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}};
        int[] colors = new int[]{strokeColor, strokeColor};
        textInputLayoutAptitude.setBoxStrokeColorStateList(new ColorStateList(states, colors));
        textInputLayoutAptitude.setDefaultHintTextColor(ColorStateList.valueOf(labelColor));
        textInputLayoutAptitude.setHintTextColor(ColorStateList.valueOf(labelColor));
    }

    private void setMode(int newMode) {
        mode = newMode;
        boolean viewOnly = mode == MODE_VIEW;

        setEditTextEnabled(editTextLastName, !viewOnly);
        setEditTextEnabled(editTextFirstName, !viewOnly);
        setEditTextEnabled(editTextDni, !viewOnly);
        setEditTextEnabled(spinnerGender, !viewOnly);
        if (editTextBirthDate != null) {
            editTextBirthDate.setEnabled(!viewOnly);
            editTextBirthDate.setFocusable(false);
            editTextBirthDate.setFocusableInTouchMode(false);
            editTextBirthDate.setClickable(!viewOnly);
        }
        setEditTextEnabled(editTextCity, !viewOnly);
        setEditTextEnabled(editTextAddress, !viewOnly);
        setEditTextEnabled(spinnerBloodType, !viewOnly);
        setEditTextEnabled(editTextWeight, !viewOnly);
        setEditTextEnabled(editTextHeight, !viewOnly);
        setEditTextEnabled(spinnerAptitude, !viewOnly);
        setEditTextEnabled(spinnerAddRestriction, !viewOnly);
        setEditTextEnabled(editTextVedaDays, !viewOnly);
        setEditTextEnabled(editTextRestrictionNotes, !viewOnly);

        if (textInputLayoutLastDonationDate != null) {
            textInputLayoutLastDonationDate.setVisibility(mode == MODE_REGISTER ? View.GONE : View.VISIBLE);
        }

        if (textInputLayoutNextEligibleDate != null && mode == MODE_REGISTER) {
            textInputLayoutNextEligibleDate.setVisibility(View.GONE);
        }
        if (textViewAnnualCounts != null && mode == MODE_REGISTER) {
            textViewAnnualCounts.setVisibility(View.GONE);
        }

        setContactRowsEnabled(layoutPhoneRows, !viewOnly);
        setContactRowsEnabled(layoutEmailRows, !viewOnly);

        buttonAddPhone.setVisibility(viewOnly ? View.GONE : View.VISIBLE);
        buttonAddEmail.setVisibility(viewOnly ? View.GONE : View.VISIBLE);

        if (viewOnly) {
            for (int i = 0; i < chipGroupRestrictions.getChildCount(); i++) {
                View child = chipGroupRestrictions.getChildAt(i);
                if (child instanceof Chip) {
                    ((Chip) child).setCloseIconVisible(false);
                }
            }
            buttonDonorAction.setText(R.string.btn_modify_donor);
        } else if (mode == MODE_MODIFY) {
            buttonDonorAction.setText(R.string.btn_save_changes);
        } else {
            buttonDonorAction.setText(R.string.btn_save_donor);
        }

        applyAptitudeState();
    }

    private void setEditTextEnabled(EditText editText, boolean enabled) {
        if (editText != null) {
            editText.setEnabled(enabled);
            editText.setFocusable(enabled);
            editText.setFocusableInTouchMode(enabled);
        }
    }

    private void setContactRowsEnabled(LinearLayout container, boolean enabled) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View row = container.getChildAt(i);
            EditText value = row.findViewById(R.id.editTextRowValue);
            if (value != null) {
                setEditTextEnabled(value, enabled);
            }
            View removeButton = row.findViewById(R.id.buttonRowRemove);
            if (removeButton != null && !enabled) {
                removeButton.setEnabled(false);
                removeButton.setVisibility(View.GONE);
            }
        }
    }

    private void recomputeReturnDate() {
        if (mode == MODE_REGISTER || mode == MODE_MODIFY) {
            int days = parseDays();
            if (days > 0) {
                LocalDate returnDate = LocalDate.now().plusDays(days);
                if (returnDate.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) {
                    returnDate = returnDate.plusDays(2);
                } else if (returnDate.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                    returnDate = returnDate.plusDays(1);
                }
                editTextVedaReturnDate.setText(returnDate.toString());
            } else {
                editTextVedaReturnDate.setText("");
            }
        }
    }

    private void addContactRow(int mode, String value, String type, boolean removable) {
        View rowView = getLayoutInflater().inflate(R.layout.item_contact_row, null, false);
        TextInputEditText valueEdit = rowView.findViewById(R.id.editTextRowValue);
        valueEdit.setText(value != null ? value : "");

        MaterialButton removeButton = rowView.findViewById(R.id.buttonRowRemove);
        if (removable) {
            removeButton.setVisibility(View.VISIBLE);
            removeButton.setOnClickListener(v -> {
                LinearLayout container = (LinearLayout) rowView.getParent();
                if (container != null) {
                    container.removeView(rowView);
                }
            });
        }

        if (mode == MODE_PHONE) {
            layoutPhoneRows.addView(rowView);
        } else {
            layoutEmailRows.addView(rowView);
        }
        setContactRowsEnabled(layoutPhoneRows, this.mode != MODE_VIEW);
        setContactRowsEnabled(layoutEmailRows, this.mode != MODE_VIEW);
    }

    private void requestedIfEmptyContacts() {
        if (layoutPhoneRows.getChildCount() == 0) {
            addContactRow(MODE_PHONE, "", null, false);
        }
        if (layoutEmailRows.getChildCount() == 0) {
            addContactRow(MODE_EMAIL, "", null, false);
        }
    }

    private void handleSave() {
        if (!validateDonorFields()) {
            return;
        }

        Donor donor = buildDonor();
        donor.setId(currentDonor != null ? currentDonor.getId() : 0);
        viewModel.saveDonor(donor);
    }

    private boolean validateDonorFields() {
        List<String> missing = new ArrayList<>();
        if (getText(editTextDni).isEmpty()) {
            missing.add("DNI");
        }
        if (getText(editTextFirstName).isEmpty()) {
            missing.add("Nombre");
        }
        if (getText(editTextLastName).isEmpty()) {
            missing.add("Apellido");
        }
        if (getText(spinnerBloodType).isEmpty()) {
            missing.add("Grupo/Factor");
        }
        if (!missing.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Por favor completá " + TextUtils.join(", ", missing),
                    Toast.LENGTH_LONG).show();
            return false;
        }
        if (getString(R.string.aptitude_no_apto).equals(getText(spinnerAptitude)) && selectedRestrictions.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Seleccioná al menos una restricción, o volvé a elegir Apto",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        if (!selectedRestrictions.isEmpty() && parseDays() <= 0) {
            Toast.makeText(requireContext(),
                    "Ingresá los días de veda para la restricción seleccionada",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private Donor buildDonor() {
        Donor donor = new Donor();
        donor.setDni(getText(editTextDni));
        donor.setFirstName(getText(editTextFirstName));
        donor.setLastName(getText(editTextLastName));
        donor.setGender(getText(spinnerGender));
        donor.setBirthDate(getText(editTextBirthDate));
        donor.setCity(getText(editTextCity));
        donor.setAddress(getText(editTextAddress));
        donor.setBloodType(getText(spinnerBloodType));
        donor.setPhenotype(getText(editTextPhenotype));
        donor.setWeight(parseNullableDouble(getText(editTextWeight)));
        donor.setHeight(parseNullableDouble(getText(editTextHeight)));

        List<ContactPhone> phones = new ArrayList<>();
        boolean primarySet = false;
        for (int i = 0; i < layoutPhoneRows.getChildCount(); i++) {
            View row = layoutPhoneRows.getChildAt(i);
            String value = textOfView(row, R.id.editTextRowValue);
            if (value.isEmpty()) continue;
            ContactPhone phone = new ContactPhone(value, "Mobile", !primarySet);
            primarySet = true;
            phones.add(phone);
        }
        donor.setPhones(phones);

        List<ContactEmail> emails = new ArrayList<>();
        boolean emailPrimarySet = false;
        for (int i = 0; i < layoutEmailRows.getChildCount(); i++) {
            View row = layoutEmailRows.getChildAt(i);
            String value = textOfView(row, R.id.editTextRowValue);
            if (value.isEmpty()) continue;
            ContactEmail email = new ContactEmail(value, "Personal", !emailPrimarySet);
            emailPrimarySet = true;
            emails.add(email);
        }
        donor.setEmails(emails);
        return donor;
    }

    private int parseDays() {
        try {
            return Integer.parseInt(getText(editTextVedaDays));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String textOfView(View view, int id) {
        EditText edit = view.findViewById(id);
        return edit != null && edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    private String getText(EditText edit) {
        return edit != null && edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String formatDecimal(Double value) {
        if (value == null) {
            return "";
        }
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf(value.intValue());
        }
        return String.valueOf(value);
    }

    private Double parseNullableDouble(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}