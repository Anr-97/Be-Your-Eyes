package xyz.lanshive.beyoureyes.model;

import com.google.gson.annotations.SerializedName;

public class StatisticsResponse {
    @SerializedName("blindUsers")
    private int blindUsers;

    @SerializedName("volunteers")
    private int volunteers;

    @SerializedName("onlineVolunteers")
    private int onlineVolunteers;

    // Getters
    public int getBlindUsers() { return blindUsers; }
    public int getVolunteers() { return volunteers; }
    public int getOnlineVolunteers() { return onlineVolunteers; }
}