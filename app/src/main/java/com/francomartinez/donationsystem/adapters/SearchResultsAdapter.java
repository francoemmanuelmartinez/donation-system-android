package com.francomartinez.donationsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.models.SearchResultItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.SearchResultViewHolder> {

    public interface OnContactDonorListener {
        void onWhatsApp(SearchResultItem item);
    }

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());

    private final List<SearchResultItem> items = new ArrayList<>();
    private final OnContactDonorListener contactListener;

    public SearchResultsAdapter(OnContactDonorListener contactListener) {
        this.contactListener = contactListener;
    }

    public void submitList(List<SearchResultItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        return new SearchResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        SearchResultItem item = items.get(position);
        holder.textViewAvatar.setText(item.getInitial());
        holder.textViewSearchResultName.setText(item.getFullName());
        holder.textViewSearchResultPhone.setText(item.getPhoneNumber() != null ? item.getPhoneNumber() : "");
        holder.textViewSearchResultLastDonation.setText(formatLastDonation(holder, item.getLastDonationDate()));
        bindState(holder, item);
        holder.buttonWhatsappDonor.setOnClickListener(v -> {
            if (contactListener != null) {
                contactListener.onWhatsApp(item);
            }
        });
    }

    private void bindState(SearchResultViewHolder holder, SearchResultItem item) {
        String state = item.getState();
        boolean hasRecovery = item.getRecoveryVedaEndDate() != null;
        boolean hasRestriction = item.getRestrictionVedaEndDate() != null;

        Chip primary = holder.chipStatePrimary;
        Chip secondary = holder.chipStateSecondary;
        TextView veda = holder.textViewSearchResultVeda;

        if (state == null || "APTO".equals(state)) {
            primary.setVisibility(View.VISIBLE);
            primary.setText(holder.itemView.getContext().getString(R.string.badge_apto));
            primary.setChipBackgroundColorResource(R.color.aptitude_green);
            secondary.setVisibility(View.GONE);
            veda.setVisibility(View.GONE);
            return;
        }

        if ("VEDA_RECUPERACION".equals(state)) {
            primary.setText(holder.itemView.getContext().getString(R.string.chip_label_recovery,
                    formatDate(item.getRecoveryVedaEndDate())));
            primary.setChipBackgroundColorResource(R.color.state_recovery);
        } else {
            primary.setText(holder.itemView.getContext().getString(R.string.chip_label_restriction,
                    formatDate(item.getRestrictionVedaEndDate())));
            primary.setChipBackgroundColorResource(R.color.veda_red);
        }
        primary.setVisibility(View.VISIBLE);

        // Secondary chip when both vedas are active (not highlighted)
        if (hasRecovery && hasRestriction) {
            secondary.setVisibility(View.VISIBLE);
            secondary.setText(holder.itemView.getContext().getString(R.string.chip_label_secondary,
                    hasRecovery
                            ? holder.itemView.getContext().getString(R.string.chip_label_recovery,
                            formatDate(item.getRecoveryVedaEndDate()))
                            : holder.itemView.getContext().getString(R.string.chip_label_restriction,
                            formatDate(item.getRestrictionVedaEndDate()))));
            secondary.setChipBackgroundColorResource(R.color.badge_grey);
        } else {
            secondary.setVisibility(View.GONE);
        }

        if (item.getRestrictionVedaReason() != null && !item.getRestrictionVedaReason().isEmpty()) {
            veda.setVisibility(View.VISIBLE);
            if (hasRecovery && hasRestriction) {
                veda.setText(holder.itemView.getContext().getString(R.string.veda_reason_combined,
                        item.getRestrictionVedaReason()));
            } else {
                veda.setText(holder.itemView.getContext().getString(R.string.veda_reason_single,
                        item.getRestrictionVedaReason()));
            }
        } else {
            veda.setVisibility(View.GONE);
        }
    }

    private String formatLastDonation(SearchResultViewHolder holder, String date) {
        if (date == null || date.trim().isEmpty()) {
            return holder.itemView.getContext().getString(R.string.label_no_donation);
        }
        String normalized = date;
        if (normalized.contains(" ")) {
            normalized = normalized.substring(0, 10);
        }
        return holder.itemView.getContext().getString(R.string.label_last_donation) + " " + formatDate(normalized);
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

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SearchResultViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewAvatar;
        final TextView textViewSearchResultName;
        final TextView textViewSearchResultPhone;
        final TextView textViewSearchResultLastDonation;
        final TextView textViewSearchResultVeda;
        final Chip chipStatePrimary;
        final Chip chipStateSecondary;
        final MaterialButton buttonWhatsappDonor;

        SearchResultViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewAvatar = itemView.findViewById(R.id.textViewAvatar);
            textViewSearchResultName = itemView.findViewById(R.id.textViewSearchResultName);
            textViewSearchResultPhone = itemView.findViewById(R.id.textViewSearchResultPhone);
            textViewSearchResultLastDonation = itemView.findViewById(R.id.textViewSearchResultLastDonation);
            textViewSearchResultVeda = itemView.findViewById(R.id.textViewSearchResultVeda);
            chipStatePrimary = itemView.findViewById(R.id.chipStatePrimary);
            chipStateSecondary = itemView.findViewById(R.id.chipStateSecondary);
            buttonWhatsappDonor = itemView.findViewById(R.id.buttonWhatsappDonor);
        }
    }
}