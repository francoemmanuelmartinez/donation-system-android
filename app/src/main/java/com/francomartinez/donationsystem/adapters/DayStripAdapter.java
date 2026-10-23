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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DayStripAdapter extends RecyclerView.Adapter<DayStripAdapter.DayViewHolder> {

    private static final String[] WEEKDAY_SHORT = {
            "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"
    };

    public interface OnDaySelectedListener {
        void onDaySelected(LocalDate day);
    }

    private final List<LocalDate> days;
    private final OnDaySelectedListener listener;
    private final Map<String, Integer> counts = new HashMap<>();
    private int selectedPosition = 0;

    public DayStripAdapter(List<LocalDate> days, OnDaySelectedListener listener) {
        this.days = days;
        this.listener = listener;
    }

    public void setCounts(Map<String, Integer> newCounts) {
        counts.clear();
        if (newCounts != null) {
            counts.putAll(newCounts);
        }
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int old = selectedPosition;
        selectedPosition = position;
        if (old >= 0 && old < days.size()) {
            notifyItemChanged(old);
        }
        if (position >= 0 && position < days.size()) {
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_strip, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        LocalDate day = days.get(position);
        holder.textViewDayStripWeekday.setText(WEEKDAY_SHORT[day.getDayOfWeek().getValue() - 1]);
        holder.textViewDayStripNumber.setText(String.valueOf(day.getDayOfMonth()));

        Integer count = counts.get(day.toString());
        if (count != null && count > 0) {
            holder.textViewDayStripCount.setVisibility(View.VISIBLE);
            holder.textViewDayStripCount.setText(String.valueOf(count));
        } else {
            holder.textViewDayStripCount.setVisibility(View.GONE);
        }

        boolean selected = position == selectedPosition;
        holder.cardDayStrip.setCardBackgroundColor(holder.itemView.getContext().getColor(
                selected ? R.color.medical_blue : R.color.surface_light));
        int textColor = holder.itemView.getContext().getColor(
                selected ? R.color.white : R.color.medical_blue);
        holder.textViewDayStripWeekday.setTextColor(textColor);
        holder.textViewDayStripNumber.setTextColor(textColor);

        holder.itemView.setOnClickListener(v -> listener.onDaySelected(day));
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardDayStrip;
        final TextView textViewDayStripWeekday;
        final TextView textViewDayStripNumber;
        final TextView textViewDayStripCount;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            cardDayStrip = itemView.findViewById(R.id.cardDayStrip);
            textViewDayStripWeekday = itemView.findViewById(R.id.textViewDayStripWeekday);
            textViewDayStripNumber = itemView.findViewById(R.id.textViewDayStripNumber);
            textViewDayStripCount = itemView.findViewById(R.id.textViewDayStripCount);
        }
    }
}
