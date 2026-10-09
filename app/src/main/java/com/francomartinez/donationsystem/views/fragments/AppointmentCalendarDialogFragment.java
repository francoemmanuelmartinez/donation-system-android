package com.francomartinez.donationsystem.views.fragments;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.adapters.MonthCalendarAdapter;
import com.francomartinez.donationsystem.models.Appointment;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.viewmodels.AppointmentsViewModel;
import com.google.android.material.button.MaterialButton;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppointmentCalendarDialogFragment extends DialogFragment {

    public interface OnDayPickedListener {
        void onDayPicked(LocalDate day, List<Appointment> monthAppointments);
    }

    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", ""));

    private MaterialButton buttonPrevMonth;
    private MaterialButton buttonNextMonth;
    private TextView textViewCalendarMonthYear;
    private RecyclerView recyclerViewMonthGrid;

    private AppointmentsViewModel viewModel;
    private MonthCalendarAdapter adapter;
    private final Map<String, List<Appointment>> monthCache = new HashMap<>();

    private YearMonth displayedMonth;
    private String lastRequestedMonth;

    private OnDayPickedListener dayPickedListener;

    public void setDayPickedListener(OnDayPickedListener listener) {
        this.dayPickedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return inflater.inflate(R.layout.dialog_appointment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        buttonPrevMonth = view.findViewById(R.id.buttonPrevMonth);
        buttonNextMonth = view.findViewById(R.id.buttonNextMonth);
        textViewCalendarMonthYear = view.findViewById(R.id.textViewCalendarMonthYear);
        recyclerViewMonthGrid = view.findViewById(R.id.recyclerViewMonthGrid);

        viewModel = new ViewModelProvider(this).get(AppointmentsViewModel.class);

        adapter = new MonthCalendarAdapter(this::onDayClick);
        recyclerViewMonthGrid.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        recyclerViewMonthGrid.setAdapter(adapter);

        displayedMonth = YearMonth.now();
        buttonPrevMonth.setOnClickListener(v -> showMonth(displayedMonth.minusMonths(1)));
        buttonNextMonth.setOnClickListener(v -> showMonth(displayedMonth.plusMonths(1)));

        viewModel.getMonthResult().observe(getViewLifecycleOwner(), this::onMonthResult);

        showMonth(displayedMonth);
    }

    private void showMonth(YearMonth month) {
        displayedMonth = month;
        textViewCalendarMonthYear.setText(capitalize(month.format(MONTH_LABEL)));

        String key = month.toString();
        List<Appointment> cached = monthCache.get(key);
        if (cached != null) {
            renderGrid(cached);
            return;
        }
        lastRequestedMonth = key;
        viewModel.loadMonthAppointments(key);
    }

    private void onMonthResult(Resource<List<Appointment>> result) {
        if (result == null) {
            return;
        }
        if (result.status == Resource.Status.SUCCESS) {
            List<Appointment> data = result.data != null ? result.data : new ArrayList<>();
            if (lastRequestedMonth != null) {
                monthCache.put(lastRequestedMonth, data);
            }
            renderGrid(data);
        } else if (result.status == Resource.Status.ERROR && result.message != null) {
            Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show();
        }
    }

    private void renderGrid(List<Appointment> monthAppointments) {
        Map<String, Integer> counts = new HashMap<>();
        if (monthAppointments != null) {
            for (Appointment appointment : monthAppointments) {
                String date = appointment.getAppointmentDate();
                if (date != null) {
                    counts.merge(date, 1, Integer::sum);
                }
            }
        }

        List<MonthCalendarAdapter.MonthDayCell> cells = new ArrayList<>();
        DayOfWeek firstDay = displayedMonth.atDay(1).getDayOfWeek();
        int leadingBlanks = firstDay.getValue() - 1;
        int daysInMonth = displayedMonth.lengthOfMonth();

        YearMonth currentMonth = displayedMonth;
        for (int i = 0; i < leadingBlanks; i++) {
            cells.add(new MonthCalendarAdapter.MonthDayCell(null, 0));
        }
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            int count = counts.getOrDefault(date.toString(), 0);
            cells.add(new MonthCalendarAdapter.MonthDayCell(date, count));
        }
        while (cells.size() % 7 != 0) {
            cells.add(new MonthCalendarAdapter.MonthDayCell(null, 0));
        }

        adapter.submitList(cells);
    }

    private void onDayClick(LocalDate day) {
        String key = displayedMonth.toString();
        List<Appointment> monthData = monthCache.get(key);
        if (dayPickedListener != null) {
            dayPickedListener.onDayPicked(day, monthData != null ? new ArrayList<>(monthData) : new ArrayList<>());
        }
        dismiss();
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getDialog() != null && getDialog().getWindow() != null) {
            Window window = getDialog().getWindow();
            window.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.94f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}