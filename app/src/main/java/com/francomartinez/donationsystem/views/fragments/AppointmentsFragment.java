package com.francomartinez.donationsystem.views.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.adapters.AppointmentsAdapter;
import com.francomartinez.donationsystem.adapters.DayFilterAdapter;
import com.francomartinez.donationsystem.models.Appointment;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.utils.SessionManager;
import com.francomartinez.donationsystem.viewmodels.AppointmentsViewModel;
import com.francomartinez.donationsystem.viewmodels.DonorsViewModel;
import com.francomartinez.donationsystem.viewmodels.SharedDonorViewModel;
import com.francomartinez.donationsystem.views.MainActivity;
import com.google.android.material.button.MaterialButton;

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

    private Integer pendingReminderAppointmentId = null;
    private String pendingReminderChannel = null;
    private String pendingAppointmentProcedure = null;

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
        buttonOpenCalendar = view.findViewById(R.id.buttonOpenCalendar);
        buttonDispatchReminders = view.findViewById(R.id.buttonDispatchReminders);
        recyclerViewDaysFilter = view.findViewById(R.id.recyclerViewDaysFilter);
        recyclerViewAppointmentsList = view.findViewById(R.id.recyclerViewAppointmentsList);

        setupWeek();
        setupRecyclers();
        observeViewModel();

        buttonScheduleDonor.setOnClickListener(v -> {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.openScheduleAppointment();
            }
        });

        buttonOpenCalendar.setOnClickListener(v -> openCalendar());
        buttonDispatchReminders.setOnClickListener(v -> handleDispatchReminders());
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
            viewModel.loadAppointments(weekDays.get(0).format(DB_DATE));
        }
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
    }

    private void onAppointmentDonorClick(Appointment appointment) {
        pendingAppointmentProcedure = appointment.getTargetProcedure();
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
            if (target.equals(appointment.getAppointmentDate())) {
                filtered.add(appointment);
            }
        }
        appointmentsAdapter.submitList(filtered);
        textViewEmptyAppointments.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
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