package com.francomartinez.donationsystem.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.adapters.ActiveUsersAdapter;
import com.francomartinez.donationsystem.models.User;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.utils.RoleMapper;
import com.francomartinez.donationsystem.viewmodels.UsersManagementViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class ActiveUsersFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_active_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        UsersManagementViewModel viewModel = new ViewModelProvider(requireActivity()).get(UsersManagementViewModel.class);

        RecyclerView recyclerViewActiveUsers = view.findViewById(R.id.recyclerViewActiveUsers);
        recyclerViewActiveUsers.setLayoutManager(new LinearLayoutManager(requireContext()));

        ActiveUsersAdapter adapter = new ActiveUsersAdapter(new ActiveUsersAdapter.OnActiveUserActionListener() {
            @Override
            public void onSelectRole(User user) {
                showRoleDialog(user, viewModel);
            }

            @Override
            public void onDelete(User user) {
                showDeleteConfirm(user, viewModel);
            }
        });
        recyclerViewActiveUsers.setAdapter(adapter);

        viewModel.getActiveUsers().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) {
                return;
            }
            if (resource.status == Resource.Status.SUCCESS) {
                List<User> users = resource.data;
                adapter.submitList(users != null ? users : java.util.Collections.emptyList());
            }
        });

        viewModel.loadActiveUsers();
    }

    private void showRoleDialog(User user, UsersManagementViewModel viewModel) {
        String[] labels = new String[]{
                getString(R.string.role_administrativa),
                getString(R.string.role_directora),
                getString(R.string.role_superadmin)
        };
        int checked = 0;
        if (RoleMapper.ROLE_DIRECTORA.equals(user.getRole())) checked = 1;
        else if (RoleMapper.ROLE_SUPERADMIN.equals(user.getRole())) checked = 2;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.hint_select_role)
                .setNegativeButton(R.string.btn_cancel, null)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    String newRole = RoleMapper.fromSpanish(labels[which]);
                    dialog.dismiss();
                    if (newRole.equals(user.getRole())) {
                        return;
                    }
                    if (RoleMapper.ROLE_DIRECTORA.equals(user.getRole()) || RoleMapper.ROLE_SUPERADMIN.equals(user.getRole())) {
                        showDemoteConfirm(user.getId(), newRole, viewModel);
                    } else {
                        viewModel.updateUserRole(user.getId(), newRole);
                    }
                })
                .show();
    }

    private void showDemoteConfirm(int userId, String newRole, UsersManagementViewModel viewModel) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.activity_users_title)
                .setMessage(R.string.dialog_confirm_demote_director)
                .setPositiveButton(R.string.btn_accept, (dialog, which) -> viewModel.updateUserRole(userId, newRole))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showDeleteConfirm(User user, UsersManagementViewModel viewModel) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.activity_users_title)
                .setMessage(R.string.dialog_confirm_delete_user)
                .setPositiveButton(R.string.btn_accept, (dialog, which) -> viewModel.deleteUser(user.getId()))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}