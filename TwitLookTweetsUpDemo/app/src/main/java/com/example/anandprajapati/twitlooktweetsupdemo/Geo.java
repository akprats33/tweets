package com.example.anandprajapati.twitlooktweetsupdemo;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Anand Prajapati on 3/4/2015.
 */
public class Geo {

    @SerializedName("type")
    private String type;

    @SerializedName("coordinates")
    private List<Double> points;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Double> getPoints() {
        return points;
    }

    public void setPoints(List<Double> points) {
        this.points = points;
    }
}
