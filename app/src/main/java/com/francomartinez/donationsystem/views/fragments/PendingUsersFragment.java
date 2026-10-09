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
import com.francomartinez.donationsystem.adapters.PendingUsersAdapter;
import com.francomartinez.donationsystem.models.User;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.utils.RoleMapper;
import com.francomartinez.donationsystem.utils.SessionManager;
import com.francomartinez.donationsystem.viewmodels.UsersManagementViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class PendingUsersFragment extends Fragment {

    private PendingUsersAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pending_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        UsersManagementViewModel viewModel = new ViewModelProvider(requireActivity()).get(UsersManagementViewModel.class);

        RecyclerView recyclerViewPendingUsers = view.findViewById(R.id.recyclerViewPendingUsers);
        recyclerViewPendingUsers.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new PendingUsersAdapter(new PendingUsersAdapter.OnPendingUserActionListener() {
            @Override
            public void onSelectRole(User user) {
                showRoleDialog(user);
            }

            @Override
            public void onApprove(User user, String role) {
                SessionManager session = SessionManager.getInstance(requireContext());
                viewModel.approveUser(user, role, session.getUserIdOrNull());
            }
        });
        recyclerViewPendingUsers.setAdapter(adapter);

        viewModel.getPendingUsers().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) {
                return;
            }
            if (resource.status == Resource.Status.SUCCESS) {
                List<User> users = resource.data;
                adapter.submitList(users != null ? users : java.util.Collections.emptyList());
            }
        });

        viewModel.loadPendingUsers();
    }

    private void showRoleDialog(User user) {
        String[] labels = new String[]{
                getString(R.string.role_administrativa),
                getString(R.string.role_directora)
        };
        int checked = RoleMapper.ROLE_DIRECTORA.equals(adapter.getSelectedRole(user)) ? 1 : 0;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.hint_select_role)
                .setPositiveButton(R.string.btn_accept, null)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    adapter.setSelectedRole(user.getId(), RoleMapper.fromSpanish(labels[which]));
                    dialog.dismiss();
                })
                .show();
    }
}