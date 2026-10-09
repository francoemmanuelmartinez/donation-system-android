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
import com.francomartinez.donationsystem.models.User;
import com.francomartinez.donationsystem.utils.RoleMapper;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PendingUsersAdapter extends ListAdapter<User, PendingUsersAdapter.PendingUserViewHolder> {

    public interface OnPendingUserActionListener {
        void onSelectRole(User user);

        void onApprove(User user, String role);
    }

    private static final DiffUtil.ItemCallback<User> DIFF_CALLBACK = new DiffUtil.ItemCallback<User>() {
        @Override
        public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.getId() == newItem.getId()
                    && Objects.equals(oldItem.getRole(), newItem.getRole())
                    && oldItem.isApproved() == newItem.isApproved();
        }
    };

    private final OnPendingUserActionListener listener;
    private final Map<Integer, String> selectedRoles = new HashMap<>();

    public PendingUsersAdapter(OnPendingUserActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public String getSelectedRole(User user) {
        return selectedRoles.getOrDefault(user.getId(), RoleMapper.ROLE_ADMINISTRATIVA);
    }

    public void setSelectedRole(int userId, String role) {
        selectedRoles.put(userId, role);
        int position = findPosition(userId);
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position);
        }
    }

    private int findPosition(int userId) {
        for (int i = 0; i < getItemCount(); i++) {
            if (getItem(i).getId() == userId) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    @NonNull
    @Override
    public PendingUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_user, parent, false);
        return new PendingUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingUserViewHolder holder, int position) {
        User user = getItem(position);
        holder.textViewInitial.setText(getInitial(user));
        holder.textViewName.setText(getFullName(user));
        holder.textViewDni.setText(holder.itemView.getContext().getString(R.string.hint_dni) + " " + (user.getDni() != null ? user.getDni() : ""));
        holder.textViewEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        holder.textViewCreated.setText(holder.itemView.getContext().getString(R.string.label_created_at) + " " + formatDate(user.getCreatedAt()));
        holder.buttonRole.setText(holder.itemView.getContext().getString(R.string.label_role) + " " + RoleMapper.toSpanish(getSelectedRole(user)));
        holder.buttonRole.setOnClickListener(v -> listener.onSelectRole(user));
        holder.buttonApprove.setOnClickListener(v -> listener.onApprove(user, getSelectedRole(user)));
    }

    private String getInitial(User user) {
        String name = user.getFirstName() != null && !user.getFirstName().isEmpty() ? user.getFirstName() : "?";
        return name.substring(0, 1).toUpperCase();
    }

    private String getFullName(User user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }

    private String formatDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return "-";
        }
        String normalized = date;
        if (normalized.contains(" ")) {
            normalized = normalized.substring(0, 10);
        }
        return normalized;
    }

    static class PendingUserViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewInitial;
        final TextView textViewName;
        final TextView textViewDni;
        final TextView textViewEmail;
        final TextView textViewCreated;
        final MaterialButton buttonRole;
        final MaterialButton buttonApprove;

        PendingUserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewInitial = itemView.findViewById(R.id.textViewPendingUserInitial);
            textViewName = itemView.findViewById(R.id.textViewPendingUserName);
            textViewDni = itemView.findViewById(R.id.textViewPendingUserDni);
            textViewEmail = itemView.findViewById(R.id.textViewPendingUserEmail);
            textViewCreated = itemView.findViewById(R.id.textViewPendingUserCreated);
            buttonRole = itemView.findViewById(R.id.buttonPendingUserRole);
            buttonApprove = itemView.findViewById(R.id.buttonApproveUser);
        }
    }
}