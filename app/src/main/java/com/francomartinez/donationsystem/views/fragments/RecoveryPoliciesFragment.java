package com.francomartinez.donationsystem.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.adapters.RecoveryPoliciesAdapter;
import com.francomartinez.donationsystem.models.RecoveryPolicy;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.viewmodels.RecoveryPoliciesViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecoveryPoliciesFragment extends Fragment {

    private static final String[] PROCEDURE_CODES = {"PLASMA", "PLATELETS", "BLOOD"};

    private TextView textViewFormTitle;
    private AutoCompleteTextView spinnerRecoveryProcedure;
    private TextInputEditText editTextRecoveryDays;
    private TextInputEditText editTextRecoveryDaysFemale;
    private TextInputEditText editTextMaxPerYear;
    private TextInputEditText editTextMaxPerYearFemale;
    private MaterialButton buttonSavePolicy;
    private MaterialButton buttonCancelEditPolicy;

    private RecoveryPoliciesAdapter adapter;
    private RecoveryPoliciesViewModel viewModel;
    private RecoveryPolicy editing;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recovery_policies, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RecoveryPoliciesViewModel.class);

        textViewFormTitle = view.findViewById(R.id.textViewRecoveryPolicyFormTitle);
        spinnerRecoveryProcedure = view.findViewById(R.id.spinnerRecoveryProcedure);
        editTextRecoveryDays = view.findViewById(R.id.editTextRecoveryDays);
        editTextRecoveryDaysFemale = view.findViewById(R.id.editTextRecoveryDaysFemale);
        editTextMaxPerYear = view.findViewById(R.id.editTextMaxPerYear);
        editTextMaxPerYearFemale = view.findViewById(R.id.editTextMaxPerYearFemale);
        buttonSavePolicy = view.findViewById(R.id.buttonSavePolicy);
        buttonCancelEditPolicy = view.findViewById(R.id.buttonCancelEditPolicy);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewRecoveryPolicies);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new RecoveryPoliciesAdapter(new RecoveryPoliciesAdapter.OnPolicyActionListener() {
            @Override
            public void onEdit(RecoveryPolicy policy) {
                startEditing(policy);
            }

            @Override
            public void onDelete(RecoveryPolicy policy) {
                confirmDelete(policy);
            }
        });
        recyclerView.setAdapter(adapter);

        buttonSavePolicy.setOnClickListener(v -> savePolicy());
        buttonCancelEditPolicy.setOnClickListener(v -> cancelEditing());

        viewModel.getPolicies().observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.status == Resource.Status.SUCCESS) {
                List<RecoveryPolicy> list = resource.data != null ? resource.data : Collections.emptyList();
                adapter.submitList(list);
                bindProcedureOptions(list);
            }
        });

        viewModel.getOperationMessage().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null || resource.status == Resource.Status.LOADING) return;
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                Toast.makeText(requireContext(), resource.data, Toast.LENGTH_LONG).show();
            } else if (resource.status == Resource.Status.ERROR && resource.message != null) {
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.loadPolicies();
    }

    private void bindProcedureOptions(List<RecoveryPolicy> existing) {
        List<String> reserved = new ArrayList<>();
        if (editing != null) {
            reserved.add(editing.getProcedureType());
        }
        for (RecoveryPolicy policy : existing) {
            reserved.add(policy.getProcedureType());
        }
        List<String> options = new ArrayList<>();
        for (String code : PROCEDURE_CODES) {
            if (!reserved.contains(code)) {
                options.add(procedureLabel(code));
            }
        }
        ArrayAdapter<String> adapterOptions = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, options);
        spinnerRecoveryProcedure.setAdapter(adapterOptions);
    }

    private void startEditing(RecoveryPolicy policy) {
        editing = policy;
        textViewFormTitle.setText(R.string.label_edit_recovery_policy);
        buttonCancelEditPolicy.setVisibility(View.VISIBLE);
        buttonSavePolicy.setText(R.string.btn_save_changes);

        spinnerRecoveryProcedure.setText(procedureLabel(policy.getProcedureType()), false);
        if (spinnerRecoveryProcedure.getAdapter() == null
                || spinnerRecoveryProcedure.getAdapter().getCount() == 0) {
            ArrayAdapter<String> single = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    Collections.singletonList(procedureLabel(policy.getProcedureType())));
            spinnerRecoveryProcedure.setAdapter(single);
        }
        spinnerRecoveryProcedure.setEnabled(false);

        editTextRecoveryDays.setText(String.valueOf(policy.getRecoveryDays()));
        editTextRecoveryDaysFemale.setText(String.valueOf(policy.getRecoveryDaysFemale()));
        editTextMaxPerYear.setText(String.valueOf(policy.getMaxPerYear()));
        editTextMaxPerYearFemale.setText(String.valueOf(policy.getMaxPerYearFemale()));
    }

    private void cancelEditing() {
        editing = null;
        textViewFormTitle.setText(R.string.label_new_recovery_policy);
        buttonCancelEditPolicy.setVisibility(View.GONE);
        buttonSavePolicy.setText(R.string.btn_save_policy);
        spinnerRecoveryProcedure.setEnabled(true);
        clearForm();
        reloadOptions();
    }

    private void reloadOptions() {
        if (adapter.getCurrentList().isEmpty()) {
            return;
        }
        bindProcedureOptions(adapter.getCurrentList());
    }

    private void savePolicy() {
        String procedureLabel = spinnerRecoveryProcedure.getText() != null
                ? spinnerRecoveryProcedure.getText().toString().trim() : "";
        String procedureCode = codeForLabel(procedureLabel);
        int recoveryDays = parseNumber(editTextRecoveryDays);
        int recoveryDaysFemale = parseNumber(editTextRecoveryDaysFemale);
        int maxPerYear = parseNumber(editTextMaxPerYear);
        int maxPerYearFemale = parseNumber(editTextMaxPerYearFemale);

        if (procedureCode.isEmpty() || recoveryDays < 0 || recoveryDaysFemale < 0
                || maxPerYear < 0 || maxPerYearFemale < 0) {
            Toast.makeText(requireContext(), R.string.msg_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        RecoveryPolicy policy;
        if (editing != null) {
            policy = editing;
        } else {
            policy = new RecoveryPolicy();
        }
        policy.setProcedureType(procedureCode);
        policy.setRecoveryDays(recoveryDays);
        policy.setRecoveryDaysFemale(recoveryDaysFemale);
        policy.setMaxPerYear(maxPerYear);
        policy.setMaxPerYearFemale(maxPerYearFemale);

        if (editing != null) {
            viewModel.updatePolicy(policy);
        } else {
            viewModel.createPolicy(policy);
        }
        clearForm();
    }

    private void confirmDelete(RecoveryPolicy policy) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.activity_recovery_policies_title)
                .setMessage(R.string.dialog_confirm_delete_recovery_policy)
                .setPositiveButton(R.string.btn_accept, (dialog, which) -> {
                    if (editing != null && editing.getId() == policy.getId()) {
                        cancelEditing();
                    }
                    viewModel.deletePolicy(policy.getId());
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void clearForm() {
        spinnerRecoveryProcedure.setText("");
        editTextRecoveryDays.setText("");
        editTextRecoveryDaysFemale.setText("");
        editTextMaxPerYear.setText("");
        editTextMaxPerYearFemale.setText("");
    }

    private int parseNumber(TextInputEditText editText) {
        return editText.getText() != null && !editText.getText().toString().trim().isEmpty()
                ? Integer.parseInt(editText.getText().toString().trim()) : 0;
    }

    private String procedureLabel(String code) {
        if (code == null) return "";
        switch (code.toUpperCase()) {
            case "PLASMA":
                return getString(R.string.procedure_plasma);
            case "PLATELETS":
                return getString(R.string.procedure_platelets);
            case "BLOOD":
                return getString(R.string.procedure_blood);
            default:
                return code;
        }
    }

    private String codeForLabel(String label) {
        if (label == null) return "";
        for (String code : PROCEDURE_CODES) {
            if (procedureLabel(code).equalsIgnoreCase(label.trim())) {
                return code;
            }
        }
        return "";
    }
}