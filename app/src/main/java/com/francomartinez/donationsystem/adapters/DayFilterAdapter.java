package com.francomartinez.donationsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.francomartinez.donationsystem.R;
import com.google.android.material.card.MaterialCardView;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class DayFilterAdapter extends RecyclerView.Adapter<DayFilterAdapter.DayViewHolder> {

    private static final String[] WEEKDAY_SHORT = {
            "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"
    };

    public interface OnDaySelectedListener {
        void onDaySelected(LocalDate day);
    }

    private final List<LocalDate> days;
    private final OnDaySelectedListener listener;
    private int selectedPosition = 0;

    public DayFilterAdapter(List<LocalDate> days, OnDaySelectedListener listener) {
        this.days = days;
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int old = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(old);
        notifyItemChanged(position);
    }

    public void clearSelection() {
        int old = selectedPosition;
        selectedPosition = -1;
        if (old >= 0) {
            notifyItemChanged(old);
        }
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_filter, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        LocalDate day = days.get(position);
        String label = WEEKDAY_SHORT[day.getDayOfWeek().getValue() - 1] + " " + day.getDayOfMonth();
        holder.textViewDayFilter.setText(String.format(Locale.getDefault(), "%s", label));

        boolean selected = position == selectedPosition;
        holder.cardDayFilter.setCardBackgroundColor(holder.itemView.getContext().getColor(
                selected ? R.color.medical_blue : R.color.surface_light));
        holder.textViewDayFilter.setTextColor(holder.itemView.getContext().getColor(
                selected ? R.color.white : R.color.medical_blue));

        holder.itemView.setOnClickListener(v -> listener.onDaySelected(day));
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardDayFilter;
        final TextView textViewDayFilter;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            cardDayFilter = itemView.findViewById(R.id.cardDayFilter);
            textViewDayFilter = itemView.findViewById(R.id.textViewDayFilter);
        }
    }
}