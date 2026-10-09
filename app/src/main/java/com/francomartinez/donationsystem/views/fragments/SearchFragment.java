package com.francomartinez.donationsystem.views.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.adapters.SearchResultsAdapter;
import com.francomartinez.donationsystem.models.SearchResultItem;
import com.francomartinez.donationsystem.viewmodels.SearchViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class SearchFragment extends Fragment {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());

    private AutoCompleteTextView spinnerSearchBloodType;
    private MaterialButton buttonSearchDonors;
    private RecyclerView recyclerViewSearchResults;

    private SearchViewModel viewModel;
    private SearchResultsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        spinnerSearchBloodType = view.findViewById(R.id.spinnerSearchBloodType);
        buttonSearchDonors = view.findViewById(R.id.buttonSearchDonors);
        recyclerViewSearchResults = view.findViewById(R.id.recyclerViewSearchResults);

        ArrayAdapter<CharSequence> bloodAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.search_blood_types, android.R.layout.simple_dropdown_item_1line);
        spinnerSearchBloodType.setAdapter(bloodAdapter);

        recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SearchResultsAdapter(this::onWhatsApp);
        recyclerViewSearchResults.setAdapter(adapter);

        buttonSearchDonors.setOnClickListener(v -> performSearch());

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getSearchResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case LOADING:
                    buttonSearchDonors.setEnabled(false);
                    break;
                case SUCCESS:
                    buttonSearchDonors.setEnabled(true);
                    adapter.submitList(resource.data);
                    if (resource.data == null || resource.data.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.msg_search_no_results, Toast.LENGTH_LONG).show();
                    }
                    break;
                case ERROR:
                    buttonSearchDonors.setEnabled(true);
                    if (resource.message != null) {
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        });
    }

    private void performSearch() {
        String selected = spinnerSearchBloodType.getText() != null
                ? spinnerSearchBloodType.getText().toString().trim() : "";
        String bloodType = requireContext().getString(R.string.search_filter_all).equals(selected) ? "" : selected;
        viewModel.searchByBloodType(bloodType);
    }

    private void onWhatsApp(SearchResultItem item) {
        String phoneDigits = item.getPhoneNumber() == null ? "" : item.getPhoneNumber().replaceAll("[^0-9]", "");
        if (phoneDigits.isEmpty()) {
            Toast.makeText(requireContext(), R.string.msg_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean enVeda = item.getState() != null && !"APTO".equals(item.getState())
                || "EN_VEDA".equals(item.getStatus());
        if (enVeda) {
            confirmWhatsAppInVeda(item);
        } else {
            sendWhatsApp(item);
        }
    }

    private void confirmWhatsAppInVeda(SearchResultItem item) {
        String reason = item.getVedaReason() != null ? item.getVedaReason() : getString(R.string.veda_generic_reason);
        String endDate = formatDate(item.getVedaEndDate());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_whatsapp_veda_title)
                .setMessage(getString(R.string.dialog_whatsapp_veda_message, reason, endDate))
                .setPositiveButton(R.string.btn_send_whatsapp, (dialog, which) -> sendWhatsApp(item))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void sendWhatsApp(SearchResultItem item) {
        String phoneDigits = item.getPhoneNumber() == null ? "" : item.getPhoneNumber().replaceAll("[^0-9]", "");
        if (phoneDigits.isEmpty()) {
            return;
        }

        String url;
        try {
            String message = getString(R.string.whatsapp_contact_format, item.getFullName());
            url = "https://wa.me/" + normalizeWaNumber(phoneDigits) + "?text="
                    + URLEncoder.encode(message, "UTF-8");
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.msg_reminder_error, Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.msg_whatsapp_not_installed, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.msg_whatsapp_not_installed, Toast.LENGTH_LONG).show();
        }
    }

    private String normalizeWaNumber(String phoneDigits) {
        if (phoneDigits == null) return "";
        if (phoneDigits.length() >= 10 && phoneDigits.length() <= 12 && !phoneDigits.startsWith("54")) {
            return "54" + phoneDigits;
        }
        return phoneDigits;
    }

    private String formatDate(String date) {
        if (date == null || date.isEmpty()) return "";
        String normalized = date.contains(" ") ? date.substring(0, 10) : date;
        try {
            return LocalDate.parse(normalized).format(DISPLAY_DATE);
        } catch (Exception e) {
            return normalized;
        }
    }
}