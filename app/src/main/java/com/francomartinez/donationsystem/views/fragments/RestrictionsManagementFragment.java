package com.francomartinez.donationsystem.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.adapters.RestrictionsAdapter;
import com.francomartinez.donationsystem.models.MedicalRestriction;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.viewmodels.RestrictionsManagementViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class RestrictionsManagementFragment extends Fragment {

    private TextInputEditText editTextNewRestrictionName;
    private TextInputEditText editTextVedaDaysCount;
    private MaterialButton buttonSaveRestriction;
    private RestrictionsManagementViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_restrictions_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RestrictionsManagementViewModel.class);

        editTextNewRestrictionName = view.findViewById(R.id.editTextNewRestrictionName);
        editTextVedaDaysCount = view.findViewById(R.id.editTextVedaDaysCount);
        buttonSaveRestriction = view.findViewById(R.id.buttonSaveRestriction);

        RecyclerView recyclerViewRestrictionsList = view.findViewById(R.id.recyclerViewRestrictionsList);
        recyclerViewRestrictionsList.setLayoutManager(new LinearLayoutManager(requireContext()));

        RestrictionsAdapter adapter = new RestrictionsAdapter(this::confirmDelete);
        recyclerViewRestrictionsList.setAdapter(adapter);

        buttonSaveRestriction.setOnClickListener(v -> saveRestriction());

        viewModel.getDictionary().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) {
                return;
            }
            if (resource.status == Resource.Status.SUCCESS) {
                List<MedicalRestriction> list = resource.data;
                adapter.submitList(list != null ? list : java.util.Collections.emptyList());
            }
        });

        viewModel.getOperationMessage().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) {
                return;
            }
            if (resource.status == Resource.Status.SUCCESS) {
                Toast.makeText(requireContext(), resource.data, Toast.LENGTH_LONG).show();
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.loadDictionary();
    }

    private void saveRestriction() {
        String name = editTextNewRestrictionName.getText() != null
                ? editTextNewRestrictionName.getText().toString().trim() : "";
        int vedaDays = 0;
        if (editTextVedaDaysCount.getText() != null && !editTextVedaDaysCount.getText().toString().trim().isEmpty()) {
            vedaDays = Integer.parseInt(editTextVedaDaysCount.getText().toString().trim());
        }

        if (name.isEmpty() || vedaDays < 0) {
            Toast.makeText(requireContext(), R.string.msg_validation_restriction, Toast.LENGTH_SHORT).show();
            return;
        }

        editTextNewRestrictionName.setText("");
        editTextVedaDaysCount.setText("");
        viewModel.createRestriction(name, vedaDays);
    }

    private void confirmDelete(MedicalRestriction restriction) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.activity_restrictions_title)
                .setMessage(R.string.dialog_confirm_delete_restriction)
                .setPositiveButton(R.string.btn_accept, (dialog, which) -> viewModel.deleteRestriction(restriction.getId()))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}