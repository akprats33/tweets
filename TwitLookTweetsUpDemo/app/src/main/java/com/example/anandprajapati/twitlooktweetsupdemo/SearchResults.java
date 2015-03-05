package com.example.anandprajapati.twitlooktweetsupdemo;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Anand Prajapati on 3/4/2015.
 */
public class SearchResults {
    @SerializedName("statuses")
    private Searches statuses;

    @SerializedName("search_metadata")
    private SearchMetadata metadata;


    public Searches getStatuses() {
        return statuses;
    }

    public void setStatuses(Searches statuses) {
        this.statuses = statuses;
    }

    public SearchMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(SearchMetadata metadata) {
        this.metadata = metadata;
    }
}
