package com.francomartinez.donationsystem.adapters;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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

public class DonorCardAdapter extends RecyclerView.Adapter<DonorCardAdapter.DonorCardViewHolder> {

    public enum Mode {
        COMPACT,
        UNIFIED
    }

    public interface OnDonorClickListener {
        void onDonorClick(SearchResultItem item);
    }

    public interface OnContactDonorListener {
        void onWhatsApp(SearchResultItem item);
    }

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());

    private final List<SearchResultItem> items = new ArrayList<>();
    private final Mode mode;
    private OnDonorClickListener donorClickListener;
    private OnContactDonorListener contactListener;
    private String query = "";

    public DonorCardAdapter(Mode mode) {
        this.mode = mode;
    }

    public void setOnDonorClickListener(OnDonorClickListener listener) {
        this.donorClickListener = listener;
    }

    public void setOnContactDonorListener(OnContactDonorListener listener) {
        this.contactListener = listener;
    }

    public void submitList(List<SearchResultItem> newItems) {
        submitList(newItems, null);
    }

    public void submitList(List<SearchResultItem> newItems, String query) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        this.query = query != null ? query : "";
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DonorCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_donor_card, parent, false);
        return new DonorCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DonorCardViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private CharSequence highlight(Context context, String dni) {
        if (dni == null) {
            return "";
        }
        String trimmedQuery = query.trim();
        if (trimmedQuery.isEmpty()) {
            return dni;
        }
        SpannableString spannable = new SpannableString(dni);
        String lowerDni = dni.toLowerCase(Locale.ROOT);
        String lowerQuery = trimmedQuery.toLowerCase(Locale.ROOT);
        int color = ContextCompat.getColor(context, R.color.medical_blue);
        int start = 0;
        while ((start = lowerDni.indexOf(lowerQuery, start)) >= 0) {
            int end = start + lowerQuery.length();
            spannable.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = end;
        }
        return spannable;
    }

    private String formatMetaLine(Context context, SearchResultItem item) {
        String phone = item.getPhoneNumber() != null ? item.getPhoneNumber().trim() : "";
        String donorDate = item.getLastDonationDate();
        String donation = (donorDate == null || donorDate.trim().isEmpty())
                ? context.getString(R.string.label_no_donation)
                : context.getString(R.string.label_last_donation) + " " + formatDate(donorDate);
        return phone.isEmpty() ? donation : phone + "  ·  " + donation;
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

    private static String procedureLabel(Context context, String procedure) {
        if (procedure == null) {
            return "";
        }
        switch (procedure.trim().toUpperCase(Locale.ROOT)) {
            case "PLASMA":
                return context.getString(R.string.procedure_plasma);
            case "PLATELETS":
                return context.getString(R.string.procedure_platelets);
            case "BLOOD":
                return context.getString(R.string.procedure_blood);
            case "PLASMA_PLATELETS":
            case "PLASMA+PLATELETS":
            case "PLATELETS+PLASMA":
                return context.getString(R.string.procedure_plasma_platelets);
            default:
                return procedure;
        }
    }

    class DonorCardViewHolder extends RecyclerView.ViewHolder {
        final View cardDonorItem;
        final TextView textViewDonorName;
        final TextView textViewDonorMeta;
        final TextView textViewDonorVeda;
        final Chip chipBloodBadge;
        final Chip chipStatePrimary;
        final Chip chipStateSecondary;
        final Chip chipUpcomingAppointment;
        final MaterialButton buttonContact;

        DonorCardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardDonorItem = itemView.findViewById(R.id.cardDonorItem);
            textViewDonorName = itemView.findViewById(R.id.textViewDonorName);
            textViewDonorMeta = itemView.findViewById(R.id.textViewDonorMeta);
            textViewDonorVeda = itemView.findViewById(R.id.textViewDonorVeda);
            chipBloodBadge = itemView.findViewById(R.id.chipBloodBadge);
            chipStatePrimary = itemView.findViewById(R.id.chipStatePrimary);
            chipStateSecondary = itemView.findViewById(R.id.chipStateSecondary);
            chipUpcomingAppointment = itemView.findViewById(R.id.chipUpcomingAppointment);
            buttonContact = itemView.findViewById(R.id.buttonContact);
        }

        void bind(SearchResultItem item) {
            Context context = itemView.getContext();
            textViewDonorName.setText(item.getFullName());

            boolean unified = mode == Mode.UNIFIED;
            if (unified) {
                textViewDonorMeta.setText(formatMetaLine(context, item));
                bindBloodGroup(item);
                bindDetailedState(item);
                buttonContact.setVisibility(View.VISIBLE);
                buttonContact.setOnClickListener(v -> {
                    if (contactListener != null) {
                        contactListener.onWhatsApp(item);
                    }
                });
            } else {
                textViewDonorMeta.setText(highlight(context, item.getDni()));
                chipBloodBadge.setVisibility(View.GONE);
                textViewDonorVeda.setVisibility(View.GONE);
                bindCompactState(item);
                buttonContact.setVisibility(View.GONE);
            }

            bindUpcomingAppointment(context, item);

            cardDonorItem.setOnClickListener(v -> {
                if (donorClickListener != null) {
                    donorClickListener.onDonorClick(item);
                }
            });
        }

        private void bindCompactState(SearchResultItem item) {
            boolean enVeda = "EN_VEDA".equals(item.getStatus());
            if (enVeda) {
                chipStatePrimary.setText(R.string.badge_veda);
                chipStatePrimary.setChipBackgroundColorResource(R.color.veda_red);
            } else {
                chipStatePrimary.setText(R.string.badge_apto);
                chipStatePrimary.setChipBackgroundColorResource(R.color.aptitude_green);
            }
            chipStatePrimary.setVisibility(View.VISIBLE);
            chipStateSecondary.setVisibility(View.GONE);
        }

        private void bindDetailedState(SearchResultItem item) {
            String state = item.getState();
            boolean hasRecovery = item.getRecoveryVedaEndDate() != null;
            boolean hasRestriction = item.getRestrictionVedaEndDate() != null;
            Context context = itemView.getContext();

            if (state == null || "APTO".equals(state)) {
                chipStatePrimary.setVisibility(View.VISIBLE);
                chipStatePrimary.setText(context.getString(R.string.badge_apto));
                chipStatePrimary.setChipBackgroundColorResource(R.color.aptitude_green);
                chipStateSecondary.setVisibility(View.GONE);
                textViewDonorVeda.setVisibility(View.GONE);
                return;
            }

            if ("VEDA_RECUPERACION".equals(state)) {
                chipStatePrimary.setText(context.getString(R.string.chip_label_recovery,
                        formatDate(item.getRecoveryVedaEndDate())));
                chipStatePrimary.setChipBackgroundColorResource(R.color.state_recovery);
            } else {
                chipStatePrimary.setText(context.getString(R.string.chip_label_restriction,
                        formatDate(item.getRestrictionVedaEndDate())));
                chipStatePrimary.setChipBackgroundColorResource(R.color.veda_red);
            }
            chipStatePrimary.setVisibility(View.VISIBLE);

            if (hasRecovery && hasRestriction) {
                chipStateSecondary.setVisibility(View.VISIBLE);
                chipStateSecondary.setText(context.getString(R.string.chip_label_secondary,
                        hasRecovery
                                ? context.getString(R.string.chip_label_recovery,
                                formatDate(item.getRecoveryVedaEndDate()))
                                : context.getString(R.string.chip_label_restriction,
                                formatDate(item.getRestrictionVedaEndDate()))));
                chipStateSecondary.setChipBackgroundColorResource(R.color.badge_grey);
            } else {
                chipStateSecondary.setVisibility(View.GONE);
            }

            if (item.getRestrictionVedaReason() != null && !item.getRestrictionVedaReason().isEmpty()) {
                textViewDonorVeda.setVisibility(View.VISIBLE);
                if (hasRecovery && hasRestriction) {
                    textViewDonorVeda.setText(context.getString(R.string.veda_reason_combined,
                            item.getRestrictionVedaReason()));
                } else {
                    textViewDonorVeda.setText(context.getString(R.string.veda_reason_single,
                            item.getRestrictionVedaReason()));
                }
            } else {
                textViewDonorVeda.setVisibility(View.GONE);
            }
        }

        private void bindBloodGroup(SearchResultItem item) {
            String bloodType = item.getBloodType();
            if (bloodType == null || bloodType.trim().isEmpty()) {
                chipBloodBadge.setVisibility(View.GONE);
            } else {
                chipBloodBadge.setText(bloodType);
                chipBloodBadge.setVisibility(View.VISIBLE);
            }
        }

        private void bindUpcomingAppointment(Context context, SearchResultItem item) {
            if (item.hasUpcomingAppointment()) {
                chipUpcomingAppointment.setVisibility(View.VISIBLE);
                chipUpcomingAppointment.setText(context.getString(R.string.label_next_appointment,
                        procedureLabel(context, item.getNextAppointmentProcedure()),
                        formatDate(item.getNextAppointmentDate())));
            } else {
                chipUpcomingAppointment.setVisibility(View.GONE);
            }
        }
    }
}
