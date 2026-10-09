package com.francomartinez.donationsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.models.MedicalRestriction;
import com.google.android.material.button.MaterialButton;

public class RestrictionsAdapter extends ListAdapter<MedicalRestriction, RestrictionsAdapter.RestrictionViewHolder> {

    public interface OnRestrictionDeleteListener {
        void onDelete(MedicalRestriction restriction);
    }

    private static final DiffUtil.ItemCallback<MedicalRestriction> DIFF_CALLBACK = new DiffUtil.ItemCallback<MedicalRestriction>() {
        @Override
        public boolean areItemsTheSame(@NonNull MedicalRestriction oldItem, @NonNull MedicalRestriction newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull MedicalRestriction oldItem, @NonNull MedicalRestriction newItem) {
            return oldItem.getId() == newItem.getId()
                    && java.util.Objects.equals(oldItem.getName(), newItem.getName())
                    && oldItem.getDefaultVedaDays() == newItem.getDefaultVedaDays();
        }
    };

    private final OnRestrictionDeleteListener listener;

    public RestrictionsAdapter(OnRestrictionDeleteListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public RestrictionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_restriction_row, parent, false);
        return new RestrictionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RestrictionViewHolder holder, int position) {
        MedicalRestriction restriction = getItem(position);
        holder.textViewName.setText(restriction.getName());
        holder.textViewVeda.setText(holder.itemView.getContext()
                .getString(R.string.label_veda_days_count, restriction.getDefaultVedaDays()));
        holder.buttonDelete.setOnClickListener(v -> listener.onDelete(restriction));
    }

    static class RestrictionViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewName;
        final TextView textViewVeda;
        final MaterialButton buttonDelete;

        RestrictionViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewRestrictionName);
            textViewVeda = itemView.findViewById(R.id.textViewRestrictionVeda);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteRestriction);
        }
    }
}