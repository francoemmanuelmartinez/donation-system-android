package com.francomartinez.donationsystem.views.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.adapters.AppointmentsAdapter;
import com.francomartinez.donationsystem.adapters.DayFilterAdapter;
import com.francomartinez.donationsystem.models.Appointment;
import com.francomartinez.donationsystem.models.requests.CancelAppointmentRequest;
import com.francomartinez.donationsystem.models.requests.MarkAttendanceRequest;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.utils.SessionManager;
import com.francomartinez.donationsystem.viewmodels.AppointmentsViewModel;
import com.francomartinez.donationsystem.viewmodels.DonorsViewModel;
import com.francomartinez.donationsystem.viewmodels.SharedDonorViewModel;
import com.francomartinez.donationsystem.views.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.net.URLEncoder;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppointmentsFragment extends Fragment {

    private static final DateTimeFormatter DB_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter WEEK_LABEL = DateTimeFormatter.ofPattern("dd/MM", Locale.getDefault());
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());
    private static final int WEEK_COUNT = 5;

    private TextView textViewWeekHeader;
    private TextView textViewSelectedDateLabel;
    private TextView textViewEmptyAppointments;
    private MaterialButton buttonScheduleDonor;
    private MaterialButton buttonToggleHistory;
    private MaterialButton buttonToggleRequests;
    private View buttonOpenCalendar;
    private MaterialButton buttonDispatchReminders;
    private RecyclerView recyclerViewDaysFilter;
    private RecyclerView recyclerViewAppointmentsList;

    private AppointmentsViewModel viewModel;
    private DonorsViewModel donorViewModel;
    private SharedDonorViewModel sharedDonorViewModel;
    private DayFilterAdapter dayFilterAdapter;
    private AppointmentsAdapter appointmentsAdapter;

    private List<LocalDate> weekDays = new ArrayList<>();
    private LocalDate selectedDay;
    private final List<Appointment> allAppointments = new ArrayList<>();
    private List<Appointment> displaySource = allAppointments;
    private boolean calendarDayPicked = false;

    private static final int VIEW_ACTIVE = 0;
    private static final int VIEW_REQUESTS = 1;
    private static final int VIEW_HISTORY = 2;
    private int viewMode = VIEW_ACTIVE;

    private Integer pendingReminderAppointmentId = null;
    private String pendingReminderChannel = null;
    private String pendingAppointmentProcedure = null;

    private Appointment pendingCancelAppointment = null;
    private Appointment pendingAttendanceAppointment = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appointments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AppointmentsViewModel.class);
        viewModel.setCurrentUserId(SessionManager.getInstance(requireContext()).getUserIdOrNull());

        donorViewModel = new ViewModelProvider(this).get(DonorsViewModel.class);
        sharedDonorViewModel = new ViewModelProvider(requireActivity()).get(SharedDonorViewModel.class);

        textViewWeekHeader = view.findViewById(R.id.textViewWeekHeader);
        textViewSelectedDateLabel = view.findViewById(R.id.textViewSelectedDateLabel);
        textViewEmptyAppointments = view.findViewById(R.id.textViewEmptyAppointments);
        buttonScheduleDonor = view.findViewById(R.id.buttonScheduleDonor);
        buttonToggleHistory = view.findViewById(R.id.buttonToggleHistory);
        buttonToggleRequests = view.findViewById(R.id.buttonToggleRequests);
        buttonOpenCalendar = view.findViewById(R.id.buttonOpenCalendar);
        buttonDispatchReminders = view.findViewById(R.id.buttonDispatchReminders);
        recyclerViewDaysFilter = view.findViewById(R.id.recyclerViewDaysFilter);
        recyclerViewAppointmentsList = view.findViewById(R.id.recyclerViewAppointmentsList);

        setupWeek();
        setupRecyclers();
        applyViewUi();
        observeViewModel();

        buttonScheduleDonor.setOnClickListener(v -> {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.openScheduleAppointment();
            }
        });

        buttonOpenCalendar.setOnClickListener(v -> openCalendar());
        buttonDispatchReminders.setOnClickListener(v -> handleDispatchReminders());
        buttonToggleHistory.setOnClickListener(v -> setViewMode(
                viewMode == VIEW_HISTORY ? VIEW_ACTIVE : VIEW_HISTORY));
        buttonToggleRequests.setOnClickListener(v -> setViewMode(
                viewMode == VIEW_REQUESTS ? VIEW_ACTIVE : VIEW_REQUESTS));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (calendarDayPicked) {
            calendarDayPicked = false;
            displaySource = allAppointments;
            textViewSelectedDateLabel.setVisibility(View.GONE);
        }
        if (viewModel != null && weekDays != null && !weekDays.isEmpty()) {
            viewModel.loadAppointments(weekDays.get(0).format(DB_DATE), currentStatusFilter());
        }
    }

    private void setViewMode(int newMode) {
        viewMode = newMode;
        applyViewUi();
        if (viewModel != null && weekDays != null && !weekDays.isEmpty()) {
            viewModel.loadAppointments(weekDays.get(0).format(DB_DATE), currentStatusFilter());
        }
    }

    private void applyViewUi() {
        boolean history = viewMode == VIEW_HISTORY;
        buttonToggleHistory.setText(history ? R.string.action_view_active : R.string.action_view_all);
        appointmentsAdapter.setShowStatusChip(history);
        appointmentsAdapter.setRequestsMode(viewMode == VIEW_REQUESTS);
        textViewWeekHeader.setText(history
                ? getString(R.string.week_header_history, weekHeaderRange())
                : getString(R.string.week_header_format, weekStartLabel(), weekEndLabel()));
        buttonDispatchReminders.setVisibility(history ? View.GONE : View.VISIBLE);
        buttonToggleRequests.setVisibility(history ? View.GONE : View.VISIBLE);
        buttonToggleRequests.setText(viewMode == VIEW_REQUESTS
                ? R.string.action_view_active
                : R.string.action_view_requests);
    }

    private String weekHeaderRange() {
        return weekStartLabel() + " – " + weekEndLabel();
    }

    private String weekStartLabel() {
        return weekDays.isEmpty() ? "" : weekDays.get(0).format(WEEK_LABEL);
    }

    private String weekEndLabel() {
        return weekDays.isEmpty() ? "" : weekDays.get(weekDays.size() - 1).format(WEEK_LABEL);
    }

    private void setupWeek() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        weekDays = new ArrayList<>();
        for (int i = 0; i < WEEK_COUNT; i++) {
            weekDays.add(monday.plusDays(i));
        }

        LocalDate friday = monday.plusDays(WEEK_COUNT - 1L);
        selectedDay = today.isBefore(monday) || today.isAfter(friday) ? monday : today;
        textViewWeekHeader.setText(getString(R.string.week_header_format,
                monday.format(WEEK_LABEL), friday.format(WEEK_LABEL)));
    }

    private void setupRecyclers() {
        recyclerViewDaysFilter.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        dayFilterAdapter = new DayFilterAdapter(weekDays, this::applyDayFilter);
        recyclerViewDaysFilter.setAdapter(dayFilterAdapter);

        int todayIdx = weekDays.indexOf(selectedDay);
        if (todayIdx < 0) todayIdx = 0;
        dayFilterAdapter.setSelectedPosition(todayIdx);

        recyclerViewAppointmentsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        appointmentsAdapter = new AppointmentsAdapter((appointment, channel) -> {
            if ("email".equals(channel)) {
                handleEmailReminder(appointment);
            } else {
                handleWhatsAppReminder(appointment);
            }
        });
        appointmentsAdapter.setOnDonorClickListener(this::onAppointmentDonorClick);
        appointmentsAdapter.setOnActionsClickListener(this::onAppointmentActionsClick);
        recyclerViewAppointmentsList.setAdapter(appointmentsAdapter);
    }

    private void observeViewModel() {
        viewModel.getAppointmentsResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS) {
                allAppointments.clear();
                if (resource.data != null) {
                    allAppointments.addAll(resource.data);
                }
                applyDayFilter(selectedDay);
            } else if (resource.status == Resource.Status.ERROR && resource.message != null) {
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getReminderResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS) {
                if (pendingReminderAppointmentId != null) {
                    for (Appointment appointment : allAppointments) {
                        if (appointment.getId() == pendingReminderAppointmentId) {
                            if ("email".equals(pendingReminderChannel)) {
                                appointment.setEmailReminderSent(true);
                            } else {
                                appointment.setReminderSent(true);
                            }
                            break;
                        }
                    }
                }
                pendingReminderAppointmentId = null;
                pendingReminderChannel = null;
                applyDayFilter(selectedDay);
                Toast.makeText(requireContext(), R.string.msg_reminder_sent, Toast.LENGTH_SHORT).show();
            } else if (resource.status == Resource.Status.ERROR && resource.message != null) {
                pendingReminderAppointmentId = null;
                pendingReminderChannel = null;
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getDispatchResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS) {
                Toast.makeText(requireContext(), R.string.msg_dispatch_launched, Toast.LENGTH_SHORT).show();
            } else if (resource.status == Resource.Status.ERROR) {
                String message = resource.message != null ? resource.message : getString(R.string.msg_dispatch_error);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        donorViewModel.getDonorResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                sharedDonorViewModel.setSelectedDonor(resource.data);
                sharedDonorViewModel.setSelectedProcedure(pendingAppointmentProcedure);
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity != null) {
                    mainActivity.openClinicalForm();
                }
            } else if (resource.status == Resource.Status.ERROR && resource.message != null) {
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getCancelResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS) {
                Appointment cancelled = pendingCancelAppointment;
                pendingCancelAppointment = null;
                showUndoSnackbar(cancelled, R.string.msg_appointment_cancelled);
                viewModel.loadAppointments(weekDays.get(0).format(DB_DATE), currentStatusFilter());
            } else if (resource.status == Resource.Status.ERROR) {
                pendingCancelAppointment = null;
                Toast.makeText(requireContext(),
                        resource.message != null ? resource.message : getString(R.string.msg_appointment_action_error),
                        Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getAttendanceResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS) {
                Appointment updated = pendingAttendanceAppointment;
                pendingAttendanceAppointment = null;
                String message = updated != null && "NO_SHOW".equals(updated.getStatus())
                        ? getString(R.string.msg_appointment_no_show)
                        : getString(R.string.msg_appointment_attended);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                viewModel.loadAppointments(weekDays.get(0).format(DB_DATE), currentStatusFilter());
            } else if (resource.status == Resource.Status.ERROR) {
                pendingAttendanceAppointment = null;
                Toast.makeText(requireContext(),
                        resource.message != null ? resource.message : getString(R.string.msg_appointment_action_error),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onAppointmentActionsClick(Appointment appointment, View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_appointment_actions, popup.getMenu());
        Menu menu = popup.getMenu();
        MenuItem attendance = menu.findItem(R.id.action_mark_attended);
        MenuItem noShow = menu.findItem(R.id.action_mark_no_show);
        // Asistencia sólo para turnos pasados (D10).
        boolean past = isPastDate(appointment.getAppointmentDate());
        attendance.setVisible(past);
        noShow.setVisible(past);
        popup.setOnMenuItemClickListener(item -> handleActionMenuItem(item, appointment));
        popup.show();
    }

    private boolean handleActionMenuItem(MenuItem item, Appointment appointment) {
        int id = item.getItemId();
        if (id == R.id.action_edit_appointment) {
            openEditAppointment(appointment);
            return true;
        }
        if (id == R.id.action_reschedule_appointment) {
            openRescheduleAppointment(appointment);
            return true;
        }
        if (id == R.id.action_mark_attended) {
            confirmAttendance(appointment, "ATTENDED");
            return true;
        }
        if (id == R.id.action_mark_no_show) {
            confirmAttendance(appointment, "NO_SHOW");
            return true;
        }
        if (id == R.id.action_cancel_appointment) {
            confirmCancel(appointment);
            return true;
        }
        return false;
    }

    private void openEditAppointment(Appointment appointment) {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) return;
        mainActivity.openEditAppointment(appointment.getId(), appointment.getDonorId(), appointment.getDonorName(),
                appointment.getDonorDni(), appointment.getAppointmentDate(), appointment.getAppointmentTime(),
                appointment.getTargetProcedure());
    }

    private void openRescheduleAppointment(Appointment appointment) {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) return;
        mainActivity.openRescheduleAppointment(appointment.getId(), appointment.getDonorId(), appointment.getDonorName(),
                appointment.getDonorDni(), appointment.getAppointmentDate(), appointment.getAppointmentTime(),
                appointment.getTargetProcedure());
    }

    private void confirmAttendance(Appointment appointment, String status) {
        int titleRes = "ATTENDED".equals(status) ? R.string.dialog_attended_title : R.string.dialog_no_show_title;
        int messageRes = "ATTENDED".equals(status) ? R.string.dialog_attended_message : R.string.dialog_no_show_message;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes)
                .setMessage(getString(messageRes, appointment.getDonorName()))
                .setNegativeButton(R.string.btn_cancel, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.btn_confirm, (d, w) -> {
                    pendingAttendanceAppointment = appointment;
                    viewModel.markAttendance(new MarkAttendanceRequest(
                            appointment.getId(), appointment.getDonorId(), status, viewModel.getCurrentUserId()));
                })
                .show();
    }

    private void confirmCancel(Appointment appointment) {
        EditText input = new EditText(requireContext());
        input.setHint(R.string.hint_cancel_reason);
        input.setMaxLines(2);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_cancel_title)
                .setMessage(getString(R.string.dialog_cancel_message, appointment.getDonorName()))
                .setView(input)
                .setNegativeButton(R.string.btn_cancel, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.btn_confirm, (d, w) -> {
                    String reason = input.getText() != null ? input.getText().toString().trim() : "";
                    pendingCancelAppointment = appointment;
                    viewModel.cancelAppointment(new CancelAppointmentRequest(
                            appointment.getId(), appointment.getDonorId(),
                            reason.isEmpty() ? null : reason, viewModel.getCurrentUserId()));
                })
                .show();
    }

    private void showUndoSnackbar(Appointment appointment, int messageRes) {
        if (appointment == null || getView() == null) return;
        Snackbar.make(getView(), messageRes, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_view_history, v -> {
                    if (viewMode != VIEW_HISTORY) {
                        setViewMode(VIEW_HISTORY);
                    }
                })
                .show();
    }

    private boolean isPastDate(String dbDate) {
        if (dbDate == null || dbDate.trim().isEmpty()) return false;
        try {
            return LocalDate.parse(dbDate.trim().substring(0, Math.min(10, dbDate.trim().length())))
                    .isBefore(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private void onAppointmentDonorClick(Appointment appointment) {
        pendingAppointmentProcedure = appointment.getTargetProcedure();
        sharedDonorViewModel.setSelectedAppointment(appointment);
        donorViewModel.getDonorById(appointment.getDonorId());
    }

    private void applyDayFilter(LocalDate day) {
        selectedDay = day;
        int idx = weekDays.indexOf(day);
        if (idx >= 0) {
            dayFilterAdapter.setSelectedPosition(idx);
            textViewSelectedDateLabel.setVisibility(View.GONE);
        } else {
            dayFilterAdapter.clearSelection();
            textViewSelectedDateLabel.setText(getString(R.string.date_label_format, day.format(DISPLAY_DATE)));
            textViewSelectedDateLabel.setVisibility(View.VISIBLE);
        }

        String target = day.format(DB_DATE);
        List<Appointment> filtered = new ArrayList<>();
        for (Appointment appointment : displaySource) {
            if (!target.equals(appointment.getAppointmentDate())) {
                continue;
            }
            if (viewMode == VIEW_REQUESTS && !hasRescheduleRequest(appointment)) {
                continue;
            }
            filtered.add(appointment);
        }
        appointmentsAdapter.submitList(filtered);
        if (filtered.isEmpty()) {
            textViewEmptyAppointments.setText(emptyStateText());
            textViewEmptyAppointments.setVisibility(View.VISIBLE);
        } else {
            textViewEmptyAppointments.setVisibility(View.GONE);
        }
    }

    private boolean hasRescheduleRequest(Appointment appointment) {
        String requestedAt = appointment.getRescheduleRequestedAt();
        return requestedAt != null && !requestedAt.trim().isEmpty() && !"null".equalsIgnoreCase(requestedAt.trim());
    }

    private int emptyStateText() {
        if (viewMode == VIEW_HISTORY) {
            return R.string.empty_day_appointments;
        }
        if (viewMode == VIEW_REQUESTS) {
            return R.string.empty_reschedule_requests;
        }
        return R.string.empty_active_appointments;
    }

    private String currentStatusFilter() {
        return viewMode == VIEW_HISTORY ? "all" : "SCHEDULED";
    }

    private void openCalendar() {
        AppointmentCalendarDialogFragment dialog = new AppointmentCalendarDialogFragment();
        dialog.setDayPickedListener(this::onCalendarDayPicked);
        dialog.show(getParentFragmentManager(), "appointment_calendar");
    }

    private void onCalendarDayPicked(LocalDate day, List<Appointment> monthAppointments) {
        calendarDayPicked = true;
        displaySource = monthAppointments;
        applyDayFilter(day);
    }

    private void handleDispatchReminders() {
        if (isReminderInFlight()) {
            return;
        }
        viewModel.dispatchReminders();
    }

    private void handleWhatsAppReminder(Appointment appointment) {
        if (isReminderInFlight()) {
            return;
        }
        String phoneDigits = appointment.getDonorPhone() == null ? "" : appointment.getDonorPhone().replaceAll("[^0-9]", "");
        if (phoneDigits.isEmpty()) {
            Toast.makeText(requireContext(), R.string.msg_reminder_error, Toast.LENGTH_LONG).show();
            return;
        }

        String url;
        try {
            url = "https://wa.me/" + normalizeWaNumber(phoneDigits) + "?text="
                    + URLEncoder.encode(reminderMessage(appointment, false), "UTF-8");
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.msg_reminder_error, Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.msg_whatsapp_not_installed, Toast.LENGTH_LONG).show();
            return;
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.msg_whatsapp_not_installed, Toast.LENGTH_LONG).show();
            return;
        }

        pendingReminderAppointmentId = appointment.getId();
        pendingReminderChannel = "whatsapp";
        viewModel.sendReminder(appointment.getId(), "whatsapp");
    }

    private void handleEmailReminder(Appointment appointment) {
        if (isReminderInFlight()) {
            return;
        }
        String donorEmail = appointment.getDonorEmail();
        if (donorEmail == null || donorEmail.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.msg_no_donor_email, Toast.LENGTH_LONG).show();
            return;
        }

        String subject = getString(R.string.email_reminder_subject);
        String body = reminderMessage(appointment, true);

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{donorEmail.trim()});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.email_reminder_subject)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.msg_email_not_available, Toast.LENGTH_LONG).show();
            return;
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.msg_email_not_available, Toast.LENGTH_LONG).show();
            return;
        }

        pendingReminderAppointmentId = appointment.getId();
        pendingReminderChannel = "email";
        viewModel.sendReminder(appointment.getId(), "email");
    }

    private boolean isReminderInFlight() {
        return pendingReminderAppointmentId != null;
    }

    private String reminderMessage(Appointment appointment, boolean email) {
        String dateText = appointment.getAppointmentDate();
        try {
            dateText = LocalDate.parse(appointment.getAppointmentDate()).format(DISPLAY_DATE);
        } catch (Exception ignored) {
        }
        int formatRes = email ? R.string.email_reminder_format : R.string.whatsapp_reminder_format;
        return getString(formatRes,
                appointment.getDonorName(),
                procedureLabel(appointment.getTargetProcedure()),
                dateText,
                appointment.getAppointmentTime());
    }

    private String normalizeWaNumber(String phoneDigits) {
        if (phoneDigits == null) return "";
        if (phoneDigits.length() >= 10 && phoneDigits.length() <= 12 && !phoneDigits.startsWith("54")) {
            return "54" + phoneDigits;
        }
        return phoneDigits;
    }

    private String procedureLabel(String code) {
        if (code == null) return "";
        switch (code.toUpperCase()) {
            case "PLASMA":
                return getString(R.string.procedure_plasma);
            case "PLATELETS":
                return getString(R.string.procedure_platelets);
            case "BLOOD":
                return getString(R.string.procedure_blood);
            default:
                return code;
        }
    }
}