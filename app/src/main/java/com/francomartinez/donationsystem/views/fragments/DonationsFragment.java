package com.francomartinez.donationsystem.views.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.models.AnnualCount;
import com.francomartinez.donationsystem.models.Donation;
import com.francomartinez.donationsystem.models.Donor;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.utils.SessionManager;
import com.francomartinez.donationsystem.viewmodels.DonationsViewModel;
import com.francomartinez.donationsystem.viewmodels.DonorsViewModel;
import com.francomartinez.donationsystem.viewmodels.SharedDonorViewModel;
import com.francomartinez.donationsystem.views.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DonationsFragment extends Fragment {

    private static final String TAG = "DonationsFragment";

    private TextView textViewDonorContextName;
    private TextView textViewDonorContextDni;
    private TextView textViewDonorContextBlood;
    private TextView textViewDonorContextStatusBadge;
    private MaterialButton buttonSeeDonorData;
    private TextInputEditText editTextHto;
    private TextInputEditText editTextPlateletsCount;
    private MaterialButtonToggleGroup toggleGroupDonationType;
    private MaterialButton buttonSaveDonation;

    private DonationsViewModel viewModel;
    private DonorsViewModel donorViewModel;
    private SharedDonorViewModel sharedDonorViewModel;
    private Donor selectedDonor;

    private boolean mutexUpdating = false;

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
        textViewDonorContextStatusBadge = view.findViewById(R.id.textViewDonorContextStatusBadge);
        buttonSeeDonorData = view.findViewById(R.id.buttonSeeDonorData);
        editTextHto = view.findViewById(R.id.editTextHto);
        editTextPlateletsCount = view.findViewById(R.id.editTextPlateletsCount);
        toggleGroupDonationType = view.findViewById(R.id.toggleGroupDonationType);
        buttonSaveDonation = view.findViewById(R.id.buttonSaveDonation);

        setupMutexLogic();
        observeViewModel();
        observeSharedDonor();

        buttonSaveDonation.setOnClickListener(v -> handleSave());
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
    }

    private void observeSharedDonor() {
        sharedDonorViewModel.getSelectedDonor().observe(getViewLifecycleOwner(), donor -> {
            selectedDonor = donor;
            if (donor == null) {
                textViewDonorContextName.setText(R.string.hint_select_donor);
                textViewDonorContextDni.setText("");
                textViewDonorContextBlood.setText("");
                textViewDonorContextStatusBadge.setVisibility(View.GONE);
                buttonSeeDonorData.setEnabled(false);
            } else {
                textViewDonorContextName.setText(buildFullName(donor));
                textViewDonorContextDni.setText(getString(R.string.hint_dni) + " " + safe(donor.getDni()));
                textViewDonorContextBlood.setText(safe(donor.getBloodType()));
                bindBadge(donor);
                buttonSeeDonorData.setEnabled(true);
            }
        });
    }

    private void bindBadge(Donor donor) {
        boolean enVeda = "EN_VEDA".equals(donor.getStatus());
        if (enVeda) {
            textViewDonorContextStatusBadge.setText(R.string.badge_veda);
            textViewDonorContextStatusBadge.setBackgroundResource(R.drawable.bg_badge_red);
        } else {
            textViewDonorContextStatusBadge.setText(R.string.badge_apto);
            textViewDonorContextStatusBadge.setBackgroundResource(R.drawable.bg_badge_green);
        }
        textViewDonorContextStatusBadge.setVisibility(View.VISIBLE);
    }

    private String buildFullName(Donor donor) {
        return (safe(donor.getLastName()) + ", " + safe(donor.getFirstName())).trim();
    }

    private void clearClinicalForm() {
        editTextHto.setText("");
        editTextPlateletsCount.setText("");
        toggleGroupDonationType.check(R.id.buttonTypePlasma);
    }

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

    private String formatDisplayDate(String dbDate) {
        try {
            return LocalDate.parse(dbDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()));
        } catch (Exception e) {
            return dbDate;
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

    private String safe(String value) {
        return value != null ? value : "";
    }
}