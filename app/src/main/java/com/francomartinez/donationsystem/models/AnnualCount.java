package com.francomartinez.donationsystem.models;

import com.google.gson.annotations.SerializedName;

public class AnnualCount {

    @SerializedName("count")
    private int count;

    @SerializedName("max")
    private int max;

    public AnnualCount() {
    }

    public AnnualCount(int count, int max) {
        this.count = count;
        this.max = max;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }
}