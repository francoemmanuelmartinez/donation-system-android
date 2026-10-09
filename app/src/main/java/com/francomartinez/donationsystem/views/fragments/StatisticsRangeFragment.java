package com.francomartinez.donationsystem.views.fragments;

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

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.models.Statistics;
import com.francomartinez.donationsystem.utils.Resource;
import com.francomartinez.donationsystem.viewmodels.StatisticsViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class StatisticsRangeFragment extends Fragment {

    private static final String ARG_RANGE = "range";

    private String range;
    private TextView textViewScorePlasma;
    private TextView textViewScorePlatelets;
    private TextView textViewScoreBlood;
    private TextView textViewScoreDonationTotal;
    private TextView textViewScoreDonorsRegistered;
    private TextView textViewScoreDonorsInVeda;
    private LineChart chartDonationsTrend;

    public static StatisticsRangeFragment newInstance(String range) {
        StatisticsRangeFragment fragment = new StatisticsRangeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_RANGE, range);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics_range, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            range = getArguments().getString(ARG_RANGE, "week");
        } else {
            range = "week";
        }

        textViewScorePlasma = view.findViewById(R.id.textViewScorePlasma);
        textViewScorePlatelets = view.findViewById(R.id.textViewScorePlatelets);
        textViewScoreBlood = view.findViewById(R.id.textViewScoreBlood);
        textViewScoreDonationTotal = view.findViewById(R.id.textViewScoreDonationTotal);
        textViewScoreDonorsRegistered = view.findViewById(R.id.textViewScoreDonorsRegistered);
        textViewScoreDonorsInVeda = view.findViewById(R.id.textViewScoreDonorsInVeda);
        chartDonationsTrend = view.findViewById(R.id.chartDonationsTrend);

        StatisticsViewModel viewModel = new ViewModelProvider(requireActivity()).get(StatisticsViewModel.class);
        viewModel.getStatistics(range).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) {
                return;
            }
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                bindStatistics(resource.data);
            } else if (resource.status == Resource.Status.ERROR) {
                chartDonationsTrend.setNoDataText(getString(R.string.chart_no_data));
                chartDonationsTrend.invalidate();
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.loadStatistics(range);
    }

    private void bindStatistics(Statistics statistics) {
        textViewScorePlasma.setText(String.valueOf(statistics.getProcedureCount("PLASMA")));
        textViewScorePlatelets.setText(String.valueOf(statistics.getProcedureCount("PLATELETS")));
        textViewScoreBlood.setText(String.valueOf(statistics.getProcedureCount("BLOOD")));
        textViewScoreDonationTotal.setText(String.valueOf(statistics.getDonationsTotal()));
        textViewScoreDonorsRegistered.setText(String.valueOf(statistics.getDonorsRegistered()));
        textViewScoreDonorsInVeda.setText(String.valueOf(statistics.getDonorsInVeda()));

        bindChart(statistics.getSeries());
    }

    private void bindChart(List<Statistics.SeriesPoint> series) {
        if (series == null || series.isEmpty()) {
            chartDonationsTrend.clear();
            chartDonationsTrend.setNoDataText(getString(R.string.chart_no_data));
            chartDonationsTrend.setNoDataTextColor(requireContext().getColor(R.color.on_surface_variant));
            chartDonationsTrend.invalidate();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < series.size(); i++) {
            Statistics.SeriesPoint point = series.get(i);
            entries.add(new Entry(i, point.getCount()));
            String date = point.getDate() != null ? point.getDate() : "";
            labels.add(date.length() > 5 ? date.substring(5) : date);
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.label_trend));
        dataSet.setColor(requireContext().getColor(R.color.clinical_dark_blue));
        dataSet.setCircleColor(requireContext().getColor(R.color.medical_blue));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(false);

        chartDonationsTrend.setData(new LineData(dataSet));
        chartDonationsTrend.getDescription().setEnabled(false);
        chartDonationsTrend.getLegend().setEnabled(false);
        chartDonationsTrend.setTouchEnabled(true);
        chartDonationsTrend.setScaleEnabled(true);
        chartDonationsTrend.setPinchZoom(true);

        XAxis xAxis = chartDonationsTrend.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(Math.min(series.size(), 6));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(requireContext().getColor(R.color.on_surface_variant));

        YAxis leftAxis = chartDonationsTrend.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setTextColor(requireContext().getColor(R.color.on_surface_variant));
        chartDonationsTrend.getAxisRight().setEnabled(false);

        chartDonationsTrend.invalidate();
    }
}