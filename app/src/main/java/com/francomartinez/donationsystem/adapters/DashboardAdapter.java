package com.francomartinez.donationsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.models.DashboardModule;

import java.util.ArrayList;
import java.util.List;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.ModuleViewHolder> {

    public interface OnModuleClickListener {
        void onModuleClick(DashboardModule module);
    }

    private final List<DashboardModule> modules = new ArrayList<>();
    private final OnModuleClickListener listener;

    public DashboardAdapter(OnModuleClickListener listener) {
        this.listener = listener;
    }

    public void setModules(List<DashboardModule> newModules) {
        modules.clear();
        if (newModules != null) {
            modules.addAll(newModules);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ModuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_module, parent, false);
        return new ModuleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModuleViewHolder holder, int position) {
        DashboardModule module = modules.get(position);
        holder.textViewModuleTitle.setText(module.getTitleRes());
        holder.imageViewModuleIcon.setImageResource(module.getIconRes());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onModuleClick(module);
            }
        });
    }

    @Override
    public int getItemCount() {
        return modules.size();
    }

    static class ModuleViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageViewModuleIcon;
        final TextView textViewModuleTitle;

        ModuleViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewModuleIcon = itemView.findViewById(R.id.imageViewModuleIcon);
            textViewModuleTitle = itemView.findViewById(R.id.textViewModuleTitle);
        }
    }
}
