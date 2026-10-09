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
import com.francomartinez.donationsystem.models.RecoveryPolicy;
import com.google.android.material.button.MaterialButton;

public class RecoveryPoliciesAdapter extends ListAdapter<RecoveryPolicy, RecoveryPoliciesAdapter.RecoveryPolicyViewHolder> {

    public interface OnPolicyActionListener {
        void onEdit(RecoveryPolicy policy);

        void onDelete(RecoveryPolicy policy);
    }

    private static final DiffUtil.ItemCallback<RecoveryPolicy> DIFF_CALLBACK = new DiffUtil.ItemCallback<RecoveryPolicy>() {
        @Override
        public boolean areItemsTheSame(@NonNull RecoveryPolicy oldItem, @NonNull RecoveryPolicy newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull RecoveryPolicy oldItem, @NonNull RecoveryPolicy newItem) {
            return java.util.Objects.equals(oldItem.getProcedureType(), newItem.getProcedureType())
                    && oldItem.getRecoveryDays() == newItem.getRecoveryDays()
                    && oldItem.getRecoveryDaysFemale() == newItem.getRecoveryDaysFemale()
                    && oldItem.getMaxPerYear() == newItem.getMaxPerYear()
                    && oldItem.getMaxPerYearFemale() == newItem.getMaxPerYearFemale();
        }
    };

    private final OnPolicyActionListener listener;

    public RecoveryPoliciesAdapter(OnPolicyActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecoveryPolicyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recovery_policy, parent, false);
        return new RecoveryPolicyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecoveryPolicyViewHolder holder, int position) {
        RecoveryPolicy policy = getItem(position);
        holder.textViewProcedure.setText(procedureLabel(holder, policy.getProcedureType()));
        holder.textViewSummary.setText(holder.itemView.getContext().getString(
                R.string.policy_summary_format,
                policy.getRecoveryDays(),
                policy.getRecoveryDaysFemale(),
                policy.getMaxPerYear(),
                policy.getMaxPerYearFemale()));
        holder.buttonEdit.setOnClickListener(v -> listener.onEdit(policy));
        holder.buttonDelete.setOnClickListener(v -> listener.onDelete(policy));
    }

    private String procedureLabel(RecoveryPolicyViewHolder holder, String code) {
        if (code == null) return "";
        switch (code.toUpperCase()) {
            case "PLASMA":
                return holder.itemView.getContext().getString(R.string.procedure_plasma);
            case "PLATELETS":
                return holder.itemView.getContext().getString(R.string.procedure_platelets);
            case "BLOOD":
                return holder.itemView.getContext().getString(R.string.procedure_blood);
            default:
                return code;
        }
    }

    static class RecoveryPolicyViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewProcedure;
        final TextView textViewSummary;
        final MaterialButton buttonEdit;
        final MaterialButton buttonDelete;

        RecoveryPolicyViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewProcedure = itemView.findViewById(R.id.textViewRecoveryPolicyProcedure);
            textViewSummary = itemView.findViewById(R.id.textViewRecoveryPolicySummary);
            buttonEdit = itemView.findViewById(R.id.buttonEditPolicy);
            buttonDelete = itemView.findViewById(R.id.buttonDeletePolicy);
        }
    }
}