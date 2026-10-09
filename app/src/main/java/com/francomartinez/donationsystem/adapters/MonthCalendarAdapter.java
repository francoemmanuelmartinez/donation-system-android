package com.francomartinez.donationsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.francomartinez.donationsystem.R;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MonthCalendarAdapter extends RecyclerView.Adapter<MonthCalendarAdapter.MonthCellViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(LocalDate day);
    }

    public static class MonthDayCell {
        public final LocalDate date;
        public final int count;

        public MonthDayCell(LocalDate date, int count) {
            this.date = date;
            this.count = count;
        }
    }

    private final List<MonthDayCell> cells = new ArrayList<>();
    private final OnDayClickListener listener;

    public MonthCalendarAdapter(OnDayClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<MonthDayCell> newCells) {
        cells.clear();
        if (newCells != null) {
            cells.addAll(newCells);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MonthCellViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_month_day, parent, false);
        return new MonthCellViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthCellViewHolder holder, int position) {
        MonthDayCell cell = cells.get(position);
        if (cell.date == null) {
            holder.textViewMonthDayNumber.setText("");
            holder.textViewMonthDayCount.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setEnabled(false);
            return;
        }
        holder.textViewMonthDayNumber.setText(String.valueOf(cell.date.getDayOfMonth()));
        if (cell.count > 0) {
            holder.textViewMonthDayCount.setVisibility(View.VISIBLE);
            holder.textViewMonthDayCount.setText(String.valueOf(cell.count));
        } else {
            holder.textViewMonthDayCount.setVisibility(View.GONE);
        }
        holder.itemView.setEnabled(true);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDayClick(cell.date);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cells.size();
    }

    static class MonthCellViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewMonthDayNumber;
        final TextView textViewMonthDayCount;

        MonthCellViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMonthDayNumber = itemView.findViewById(R.id.textViewMonthDayNumber);
            textViewMonthDayCount = itemView.findViewById(R.id.textViewMonthDayCount);
        }
    }
}