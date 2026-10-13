package com.francomartinez.donationsystem.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class SearchPage {

    @SerializedName("items")
    private List<SearchResultItem> items;

    @SerializedName("hasMore")
    private boolean hasMore;

    public SearchPage() {
    }

    public List<SearchResultItem> getItems() {
        return items != null ? items : new ArrayList<>();
    }

    public void setItems(List<SearchResultItem> items) {
        this.items = items;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}
