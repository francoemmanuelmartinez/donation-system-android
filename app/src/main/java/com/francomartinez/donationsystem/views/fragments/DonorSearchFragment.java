package com.francomartinez.donationsystem.views.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.viewmodels.DonorSearchViewModel;
import com.francomartinez.donationsystem.viewmodels.DonorsViewModel;
import com.francomartinez.donationsystem.viewmodels.SharedDonorViewModel;
import com.francomartinez.donationsystem.views.MainActivity;
import com.google.android.material.button.MaterialButton;

import java.util.Collections;
import java.util.List;

public class DonorSearchFragment extends Fragment {

    private EditText editTextDonorSearchDni;
    private MaterialButton buttonSearchDonorsByDni;
    private RecyclerView recyclerViewDonorSearch;

    private DonorSearchViewModel searchViewModel;
    private DonorsViewModel donorViewModel;
    private SharedDonorViewModel sharedDonorViewModel;
    private DonorCardAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_donor_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editTextDonorSearchDni = view.findViewById(R.id.editTextDonorSearchDni);
        buttonSearchDonorsByDni = view.findViewById(R.id.buttonSearchDonorsByDni);
        recyclerViewDonorSearch = view.findViewById(R.id.recyclerViewDonorSearch);

        searchViewModel = new ViewModelProvider(this).get(DonorSearchViewModel.class);
        donorViewModel = new ViewModelProvider(this).get(DonorsViewModel.class);
        sharedDonorViewModel = new ViewModelProvider(requireActivity()).get(SharedDonorViewModel.class);

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

        searchViewModel.getSearchResult().observe(getViewLifecycleOwner(), result -> onSearchResult(result));
        donorViewModel.getDonorResult().observe(getViewLifecycleOwner(), result -> onDonorResult(result));
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

    private void onDonorCardClick(SearchResultItem item) {
        donorViewModel.getDonorByDni(item.getDni());
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

    private void onDonorResult(Resource<Donor> result) {
        if (result == null) {
            return;
        }
        if (result.status == Resource.Status.SUCCESS && result.data != null) {
            sharedDonorViewModel.setSelectedDonor(result.data);
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.openClinicalForm();
            }
        } else if (result.status == Resource.Status.ERROR) {
            Toast.makeText(requireContext(),
                    result.message != null ? result.message : getString(R.string.msg_donor_search_empty),
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