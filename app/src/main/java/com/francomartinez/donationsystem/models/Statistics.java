package com.francomartinez.donationsystem.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Statistics {

    @SerializedName("range")
    private String range;

    @SerializedName("fromDate")
    private String fromDate;

    @SerializedName("toDate")
    private String toDate;

    @SerializedName("donationsTotal")
    private int donationsTotal;

    @SerializedName("procedureCounts")
    private Map<String, Integer> procedureCounts;

    @SerializedName("donorsRegistered")
    private int donorsRegistered;

    @SerializedName("donorsInVeda")
    private int donorsInVeda;

    @SerializedName("series")
    private List<SeriesPoint> series;

    public Statistics() {
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public int getDonationsTotal() {
        return donationsTotal;
    }

    public void setDonationsTotal(int donationsTotal) {
        this.donationsTotal = donationsTotal;
    }

    public Map<String, Integer> getProcedureCounts() {
        if (procedureCounts == null) {
            procedureCounts = new HashMap<>();
        }
        return procedureCounts;
    }

    public void setProcedureCounts(Map<String, Integer> procedureCounts) {
        this.procedureCounts = procedureCounts;
    }

    public int getProcedureCount(String type) {
        Integer count = getProcedureCounts().get(type);
        return count != null ? count : 0;
    }

    public int getDonorsRegistered() {
        return donorsRegistered;
    }

    public void setDonorsRegistered(int donorsRegistered) {
        this.donorsRegistered = donorsRegistered;
    }

    public int getDonorsInVeda() {
        return donorsInVeda;
    }

    public void setDonorsInVeda(int donorsInVeda) {
        this.donorsInVeda = donorsInVeda;
    }

    public List<SeriesPoint> getSeries() {
        if (series == null) {
            series = new ArrayList<>();
        }
        return series;
    }

    public void setSeries(List<SeriesPoint> series) {
        this.series = series;
    }

    public static class SeriesPoint {

        @SerializedName("date")
        private String date;

        @SerializedName("count")
        private int count;

        public SeriesPoint() {
        }

        public SeriesPoint(String date, int count) {
            this.date = date;
            this.count = count;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}