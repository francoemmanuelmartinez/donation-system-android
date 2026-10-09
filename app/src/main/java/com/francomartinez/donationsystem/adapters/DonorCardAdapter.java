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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DonorCardAdapter extends RecyclerView.Adapter<DonorCardAdapter.DonorCardViewHolder> {

    public interface OnDonorClickListener {
        void onDonorClick(SearchResultItem item);
    }

    private final List<SearchResultItem> items = new ArrayList<>();
    private final OnDonorClickListener listener;
    private String query = "";

    public DonorCardAdapter(OnDonorClickListener listener) {
        this.listener = listener;
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
        SearchResultItem item = items.get(position);
        holder.textViewDonorCardName.setText(item.getFullName());
        holder.textViewDonorCardDni.setText(highlight(holder.itemView.getContext(), item.getDni()));

        boolean enVeda = "EN_VEDA".equals(item.getStatus());
        if (enVeda) {
            holder.textViewDonorCardStatusBadge.setText(R.string.badge_veda);
            holder.textViewDonorCardStatusBadge.setBackgroundResource(R.drawable.bg_badge_red);
        } else {
            holder.textViewDonorCardStatusBadge.setText(R.string.badge_apto);
            holder.textViewDonorCardStatusBadge.setBackgroundResource(R.drawable.bg_badge_green);
        }

        holder.cardDonorItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDonorClick(item);
            }
        });
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

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DonorCardViewHolder extends RecyclerView.ViewHolder {
        final View cardDonorItem;
        final TextView textViewDonorCardName;
        final TextView textViewDonorCardDni;
        final TextView textViewDonorCardStatusBadge;

        DonorCardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardDonorItem = itemView.findViewById(R.id.cardDonorItem);
            textViewDonorCardName = itemView.findViewById(R.id.textViewDonorCardName);
            textViewDonorCardDni = itemView.findViewById(R.id.textViewDonorCardDni);
            textViewDonorCardStatusBadge = itemView.findViewById(R.id.textViewDonorCardStatusBadge);
        }
    }
}