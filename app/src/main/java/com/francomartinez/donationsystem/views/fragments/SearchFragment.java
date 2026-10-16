package com.francomartinez.donationsystem.views.fragments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
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
import com.francomartinez.donationsystem.viewmodels.DonorsViewModel;
import com.francomartinez.donationsystem.viewmodels.SearchViewModel;
import com.francomartinez.donationsystem.viewmodels.SharedDonorViewModel;
import com.francomartinez.donationsystem.views.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class SearchFragment extends Fragment {

    private static final long DEBOUNCE_MS = 350;
    private static final int MIN_CHARS = 2;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());

    private TextInputEditText editTextSearchText;
    private MaterialButton buttonToggleFilters;
    private LinearLayout layoutSearchFilters;
    private AutoCompleteTextView spinnerSearchGroup;
    private AutoCompleteTextView spinnerSearchFactor;
    private RecyclerView recyclerViewSearchResults;

    private SearchViewModel searchViewModel;
    private DonorsViewModel donorViewModel;
    private SharedDonorViewModel sharedDonorViewModel;
    private DonorCardAdapter adapter;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private final Runnable debounceRunnable = this::onDebouncedSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        donorViewModel = new ViewModelProvider(this).get(DonorsViewModel.class);
        sharedDonorViewModel = new ViewModelProvider(requireActivity()).get(SharedDonorViewModel.class);

        editTextSearchText = view.findViewById(R.id.editTextSearchText);
        buttonToggleFilters = view.findViewById(R.id.buttonToggleFilters);
        layoutSearchFilters = view.findViewById(R.id.layoutSearchFilters);
        spinnerSearchGroup = view.findViewById(R.id.spinnerSearchGroup);
        spinnerSearchFactor = view.findViewById(R.id.spinnerSearchFactor);
        recyclerViewSearchResults = view.findViewById(R.id.recyclerViewSearchResults);

        ArrayAdapter<CharSequence> groupAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.search_blood_groups, android.R.layout.simple_dropdown_item_1line);
        spinnerSearchGroup.setAdapter(groupAdapter);

        ArrayAdapter<CharSequence> factorAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.search_blood_factors, android.R.layout.simple_dropdown_item_1line);
        spinnerSearchFactor.setAdapter(factorAdapter);

        recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DonorCardAdapter(DonorCardAdapter.Mode.UNIFIED);
        adapter.setOnDonorClickListener(this::onDonorCardClick);
        adapter.setOnContactDonorListener(this::onWhatsApp);
        recyclerViewSearchResults.setAdapter(adapter);
        recyclerViewSearchResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                maybeLoadMore();
            }
        });

        buttonToggleFilters.setOnClickListener(v -> toggleFilters());

        editTextSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                debounceHandler.removeCallbacks(debounceRunnable);
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
            }
        });

        editTextSearchText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                debounceHandler.removeCallbacks(debounceRunnable);
                performSearch(true);
                return true;
            }
            return false;
        });

        spinnerSearchGroup.setOnItemClickListener((parent, v, position, id) -> performSearch(false));
        spinnerSearchFactor.setOnItemClickListener((parent, v, position, id) -> performSearch(false));

        searchViewModel.getSearchResult().observe(getViewLifecycleOwner(), this::onSearchResult);
        donorViewModel.getDonorResult().observe(getViewLifecycleOwner(), this::onDonorResult);

        // Carga inicial: listado completo (aptos primero).
        searchViewModel.search("", "", "");
    }

    private void onDebouncedSearch() {
        String text = currentText();
        if (text.isEmpty() || text.length() >= MIN_CHARS) {
            performSearch(false);
        }
    }

    private void performSearch(boolean hideKeyboard) {
        if (hideKeyboard) {
            hideKeyboard();
        }
        String all = requireContext().getString(R.string.search_filter_all);
        String group = spinnerSearchGroup.getText() != null ? spinnerSearchGroup.getText().toString().trim() : "";
        String factor = spinnerSearchFactor.getText() != null ? spinnerSearchFactor.getText().toString().trim() : "";
        if (all.equalsIgnoreCase(group)) {
            group = "";
        }
        if (all.equalsIgnoreCase(factor)) {
            factor = "";
        }
        searchViewModel.search(currentText(), group, factor);
    }

    private String currentText() {
        return editTextSearchText.getText() != null ? editTextSearchText.getText().toString().trim() : "";
    }

    private void toggleFilters() {
        boolean visible = layoutSearchFilters.getVisibility() == View.VISIBLE;
        layoutSearchFilters.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    private void maybeLoadMore() {
        if (!searchViewModel.hasMore()) {
            return;
        }
        LinearLayoutManager lm = (LinearLayoutManager) recyclerViewSearchResults.getLayoutManager();
        if (lm == null) {
            return;
        }
        int lastVisible = lm.findLastVisibleItemPosition();
        int total = adapter.getItemCount();
        if (total > 0 && lastVisible >= total - 3) {
            searchViewModel.loadNextPage();
        }
    }

    private void onSearchResult(Resource<List<SearchResultItem>> resource) {
        if (resource == null) {
            return;
        }
        switch (resource.status) {
            case LOADING:
                break;
            case SUCCESS:
                List<SearchResultItem> items = resource.data;
                adapter.submitList(items, currentText());
                if (items == null || items.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.msg_search_no_results, Toast.LENGTH_SHORT).show();
                }
                break;
            case ERROR:
                if (resource.message != null) {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void onDonorCardClick(SearchResultItem item) {
        sharedDonorViewModel.setSelectedAppointment(null);
        donorViewModel.getDonorByDni(item.getDni());
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
                    result.message != null ? result.message : getString(R.string.msg_search_no_results),
                    Toast.LENGTH_LONG).show();
        }
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

    private void hideKeyboard() {
        if (getContext() == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && editTextSearchText.getWindowToken() != null) {
            imm.hideSoftInputFromWindow(editTextSearchText.getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroyView() {
        debounceHandler.removeCallbacks(debounceRunnable);
        super.onDestroyView();
    }
}
