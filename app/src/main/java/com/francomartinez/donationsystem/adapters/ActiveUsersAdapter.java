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

import java.util.Objects;

public class ActiveUsersAdapter extends ListAdapter<User, ActiveUsersAdapter.ActiveUserViewHolder> {

    public interface OnActiveUserActionListener {
        void onSelectRole(User user);

        void onDelete(User user);
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
                    && Objects.equals(oldItem.getEmail(), newItem.getEmail());
        }
    };

    private final OnActiveUserActionListener listener;

    public ActiveUsersAdapter(OnActiveUserActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActiveUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_active_user, parent, false);
        return new ActiveUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActiveUserViewHolder holder, int position) {
        User user = getItem(position);
        holder.textViewInitial.setText(getInitial(user));
        holder.textViewName.setText(getFullName(user));
        holder.textViewDni.setText(holder.itemView.getContext().getString(R.string.hint_dni) + " " + (user.getDni() != null ? user.getDni() : ""));
        holder.textViewEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        holder.buttonRole.setText(holder.itemView.getContext().getString(R.string.label_role) + " " + RoleMapper.toSpanish(user.getRole()));
        holder.buttonRole.setOnClickListener(v -> listener.onSelectRole(user));
        holder.buttonDelete.setOnClickListener(v -> listener.onDelete(user));
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

    static class ActiveUserViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewInitial;
        final TextView textViewName;
        final TextView textViewDni;
        final TextView textViewEmail;
        final MaterialButton buttonRole;
        final MaterialButton buttonDelete;

        ActiveUserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewInitial = itemView.findViewById(R.id.textViewActiveUserInitial);
            textViewName = itemView.findViewById(R.id.textViewActiveUserName);
            textViewDni = itemView.findViewById(R.id.textViewActiveUserDni);
            textViewEmail = itemView.findViewById(R.id.textViewActiveUserEmail);
            buttonRole = itemView.findViewById(R.id.buttonActiveUserRole);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteUser);
        }
    }
}